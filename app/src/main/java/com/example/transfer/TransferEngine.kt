package com.example.transfer

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object TransferEngine {
    private const val TAG = "TransferEngine"
    const val DEFAULT_PORT = 8988
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer
    private const val PROTOCOL_HEADER = "GMK_TRANSFER_V1"

    val isCancelled = AtomicBoolean(false)
    val isPaused = AtomicBoolean(false)

    fun getLocalIpAddress(context: Context): String {
        try {
            // First try WifiManager IP
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIp = wm?.connectionInfo?.ipAddress ?: 0
            if (wifiIp != 0) {
                val ip = String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    wifiIp and 0xff,
                    wifiIp shr 8 and 0xff,
                    wifiIp shr 16 and 0xff,
                    wifiIp shr 24 and 0xff
                )
                if (ip != "0.0.0.0") return ip
            }

            // Fallback to NetworkInterface iteration (Hotspot / P2P / Eth)
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining IP: ${e.message}")
        }
        return "127.0.0.1"
    }

    fun getReceivedDirectory(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, "GMK_Received")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Resolves duplicate filenames safely to avoid overwriting existing files.
     */
    fun resolveUniqueFile(directory: File, baseName: String): File {
        var target = File(directory, baseName)
        if (!target.exists()) return target

        val dotIdx = baseName.lastIndexOf('.')
        val namePart = if (dotIdx > 0) baseName.substring(0, dotIdx) else baseName
        val extPart = if (dotIdx > 0) baseName.substring(dotIdx) else ""

        var counter = 1
        while (target.exists()) {
            target = File(directory, "$namePart ($counter)$extPart")
            counter++
        }
        return target
    }

    /**
     * Check available free storage on the target partition.
     */
    fun getAvailableFreeSpace(directory: File): Long {
        return try {
            directory.usableSpace
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    /**
     * Computes or verifies SHA-256 for a given file in bounded chunks.
     */
    fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Executes sender workflow over streaming TCP connection.
     */
    suspend fun sendFiles(
        serverIp: String,
        port: Int,
        pin: String,
        senderDeviceName: String,
        files: List<File>,
        onProgress: (transferredBytes: Long, totalBytes: Long, speedBps: Long, currentFile: String, fileProgress: Float) -> Unit,
        onStatusChange: (TransferStatus, String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        isPaused.set(false)

        var socket: Socket? = null
        try {
            onStatusChange(TransferStatus.CONNECTING, "Connecting to receiver at $serverIp:$port...")
            socket = Socket(serverIp, port)
            socket.soTimeout = 45000 // 45 seconds read timeout
            socket.tcpNoDelay = true

            val writer = PrintWriter(BufferedOutputStream(socket.getOutputStream()), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // 1. Send Handshake
            val totalBytes = files.sumOf { it.length() }
            val filesJsonArray = JSONArray()
            files.forEach { file ->
                val fObj = JSONObject().apply {
                    put("name", file.name)
                    put("size", file.length())
                }
                filesJsonArray.put(fObj)
            }

            val handshakeObj = JSONObject().apply {
                put("protocol", PROTOCOL_HEADER)
                put("type", "HANDSHAKE")
                put("senderName", senderDeviceName)
                put("pin", pin)
                put("fileCount", files.size)
                put("totalBytes", totalBytes)
                put("files", filesJsonArray)
            }

            onStatusChange(TransferStatus.WAITING_APPROVAL, "Waiting for receiver to accept...")
            writer.println(handshakeObj.toString())

            // 2. Read Approval Response
            val responseLine = reader.readLine()
                ?: return@withContext Result.failure(Exception("Receiver closed connection unexpectedly."))

            val responseObj = JSONObject(responseLine)
            val authStatus = responseObj.optString("status", "REJECTED")
            if (authStatus != "ACCEPTED") {
                val reason = responseObj.optString("reason", "Receiver declined transfer.")
                onStatusChange(TransferStatus.FAILED, reason)
                return@withContext Result.failure(Exception(reason))
            }

            onStatusChange(TransferStatus.IN_PROGRESS, "Transfer started...")

            // 3. Stream files sequentially
            val outStream = socket.getOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var totalTransferred = 0L
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L

            for (file in files) {
                if (isCancelled.get()) {
                    writer.println("CMD:CANCEL")
                    onStatusChange(TransferStatus.CANCELLED, "Transfer cancelled by user.")
                    return@withContext Result.failure(Exception("Transfer cancelled."))
                }

                // Checkpoint check - send file header
                val fileHeader = JSONObject().apply {
                    put("cmd", "FILE_START")
                    put("name", file.name)
                    put("size", file.length())
                }
                writer.println(fileHeader.toString())

                var fileTransferred = 0L
                val fileSize = file.length()

                FileInputStream(file).use { fis ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled.get()) {
                            onStatusChange(TransferStatus.CANCELLED, "Transfer cancelled.")
                            return@withContext Result.failure(Exception("Cancelled."))
                        }

                        while (isPaused.get()) {
                            onStatusChange(TransferStatus.PAUSED, "Transfer paused.")
                            Thread.sleep(300)
                        }

                        outStream.write(buffer, 0, bytesRead)
                        fileTransferred += bytesRead
                        totalTransferred += bytesRead
                        bytesSinceLastSample += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            currentSpeed = (bytesSinceLastSample * 1000L) / elapsed
                            bytesSinceLastSample = 0L
                            lastTime = now

                            val fileProg = if (fileSize > 0) fileTransferred.toFloat() / fileSize else 1f
                            onProgress(totalTransferred, totalBytes, currentSpeed, file.name, fileProg)
                        }
                    }
                }
                outStream.flush()

                // Wait for FILE_ACK from receiver
                val ack = reader.readLine()
                if (ack == null || !ack.contains("FILE_ACK")) {
                    Log.w(TAG, "File ACK missing or unverified for ${file.name}")
                }
            }

            writer.println(JSONObject().apply { put("cmd", "ALL_COMPLETE") }.toString())
            onProgress(totalBytes, totalBytes, 0L, "Done", 1f)
            onStatusChange(TransferStatus.COMPLETED, "Transfer complete! All files sent successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Send error: ${e.message}", e)
            onStatusChange(TransferStatus.FAILED, "Error: ${e.message}")
            Result.failure(e)
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Helper to format human-readable speed.
     */
    fun formatSpeed(speedBytesPerSec: Long): String {
        return when {
            speedBytesPerSec >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB/s", speedBytesPerSec / (1024.0 * 1024.0 * 1024.0))
            speedBytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", speedBytesPerSec / (1024.0 * 1024.0))
            speedBytesPerSec >= 1024 -> String.format(Locale.US, "%.0f KB/s", speedBytesPerSec / 1024.0)
            speedBytesPerSec > 0 -> "$speedBytesPerSec B/s"
            else -> "0 KB/s"
        }
    }

    fun formatEta(remainingBytes: Long, speedBytesPerSec: Long): String {
        if (speedBytesPerSec <= 0 || remainingBytes <= 0) return "--"
        val seconds = remainingBytes / speedBytesPerSec
        return when {
            seconds >= 3600 -> String.format(Locale.US, "%dh %02dm", seconds / 3600, (seconds % 3600) / 60)
            seconds >= 60 -> String.format(Locale.US, "%dm %02ds", seconds / 60, seconds % 60)
            else -> "${seconds}s"
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
