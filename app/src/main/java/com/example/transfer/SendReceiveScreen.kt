package com.example.transfer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.filemanager.FileManagerHelper
import com.example.filemanager.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransferScreenMode {
    MAIN,
    SELECT_FILES,
    CONNECT_PEER,
    TRANSFER_ACTIVE,
    RECEIVE_WAITING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendReceiveScreen(
    initialPreselectedFiles: List<File> = emptyList(),
    onOpenFile: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentMode by remember { mutableStateOf(TransferScreenMode.MAIN) }

    // Networking details
    var localIp by remember { mutableStateOf(TransferEngine.getLocalIpAddress(context)) }
    var targetIpInput by remember { mutableStateOf(localIp) }
    var targetPortInput by remember { mutableStateOf(TransferEngine.DEFAULT_PORT.toString()) }
    var pairingPin by remember { mutableStateOf("7492") }

    // Files selection
    val selectedFiles = remember { mutableStateListOf<File>() }

    LaunchedEffect(initialPreselectedFiles) {
        if (initialPreselectedFiles.isNotEmpty()) {
            selectedFiles.clear()
            selectedFiles.addAll(initialPreselectedFiles)
            currentMode = TransferScreenMode.CONNECT_PEER
        }
    }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var allDeviceFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var receivedFilesList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    // Transfer status
    var transferStatus by remember { mutableStateOf(TransferStatus.PENDING) }
    var transferStatusMessage by remember { mutableStateOf("") }
    var currentFileName by remember { mutableStateOf("") }
    var transferredBytes by remember { mutableLongStateOf(0L) }
    var totalBytesToTransfer by remember { mutableLongStateOf(0L) }
    var currentSpeedBps by remember { mutableLongStateOf(0L) }
    var isSenderSession by remember { mutableStateOf(true) }
    var isPausedState by remember { mutableStateOf(false) }

    // Receiver server state
    var receiverServerJob by remember { mutableStateOf<Job?>(null) }
    var activeServerSocket by remember { mutableStateOf<ServerSocket?>(null) }
    var incomingApprovalPending by remember { mutableStateOf(false) }
    var incomingSenderName by remember { mutableStateOf("") }
    var incomingFileCount by remember { mutableIntStateOf(0) }
    var incomingTotalBytes by remember { mutableLongStateOf(0L) }
    var incomingApprovalCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    // Rename / Delete dialog
    var itemToDelete by remember { mutableStateOf<MediaItem?>(null) }
    var itemToRename by remember { mutableStateOf<MediaItem?>(null) }
    var newRenameInput by remember { mutableStateOf("") }

    fun refreshReceivedFiles() {
        scope.launch(Dispatchers.IO) {
            val dir = TransferEngine.getReceivedDirectory()
            val list = dir.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }
                ?.map { FileManagerHelper.fileToMediaItem(it) }
                ?.sortedByDescending { it.lastModified } ?: emptyList()
            withContext(Dispatchers.Main) {
                receivedFilesList = list
            }
        }
    }

    LaunchedEffect(Unit) {
        localIp = TransferEngine.getLocalIpAddress(context)
        targetIpInput = localIp
        refreshReceivedFiles()
        // Load files for sending
        scope.launch(Dispatchers.IO) {
            val vids = MediaStoreHelper.queryVideos(context)
            val auds = MediaStoreHelper.queryAudios(context)
            val archs = MediaStoreHelper.queryArchives(context)
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val docs = mutableListOf<MediaItem>()
            if (downloads.exists()) {
                downloads.walkTopDown().maxDepth(2).forEach { f ->
                    if (f.isFile && f.length() > 0) {
                        docs.add(FileManagerHelper.fileToMediaItem(f))
                    }
                }
            }
            withContext(Dispatchers.Main) {
                allDeviceFiles = (vids + auds + archs + docs).distinctBy { it.path }.sortedByDescending { it.lastModified }
            }
        }
    }

    // Stop server on leave
    DisposableEffect(Unit) {
        onDispose {
            TransferEngine.isCancelled.set(true)
            try {
                activeServerSocket?.close()
            } catch (_: Exception) {}
            receiverServerJob?.cancel()
        }
    }

    fun startReceiverServer() {
        try {
            activeServerSocket?.close()
        } catch (_: Exception) {}
        receiverServerJob?.cancel()
        TransferEngine.isCancelled.set(false)
        TransferEngine.isPaused.set(false)

        receiverServerJob = scope.launch(Dispatchers.IO) {
            var serverSocket: ServerSocket? = null
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(TransferEngine.DEFAULT_PORT))
                }
                activeServerSocket = serverSocket
                withContext(Dispatchers.Main) {
                    transferStatusMessage = "Receiver listening on $localIp:${TransferEngine.DEFAULT_PORT}"
                }

                while (isActive && !TransferEngine.isCancelled.get()) {
                    val clientSocket = serverSocket.accept()
                    clientSocket.tcpNoDelay = true

                    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                    val writer = PrintWriter(BufferedOutputStream(clientSocket.getOutputStream()), true)

                    val handshakeStr = reader.readLine()
                    if (handshakeStr == null) {
                        clientSocket.close()
                        continue
                    }

                    val handshakeObj = JSONObject(handshakeStr)
                    val senderName = handshakeObj.optString("senderName", "Nearby Device")
                    val fileCount = handshakeObj.optInt("fileCount", 0)
                    val totalBytes = handshakeObj.optLong("totalBytes", 0L)

                    val receivedDir = TransferEngine.getReceivedDirectory()
                    val freeSpace = TransferEngine.getAvailableFreeSpace(receivedDir)

                    if (freeSpace < totalBytes) {
                        val resp = JSONObject().apply {
                            put("status", "REJECTED")
                            put("reason", "Insufficient storage space on receiver. Required: ${TransferEngine.formatBytes(totalBytes)}, Available: ${TransferEngine.formatBytes(freeSpace)}")
                        }
                        writer.println(resp.toString())
                        clientSocket.close()
                        continue
                    }

                    // Request User Approval on UI
                    var isApproved = false
                    val approvalSignal = java.util.concurrent.CountDownLatch(1)

                    withContext(Dispatchers.Main) {
                        incomingSenderName = senderName
                        incomingFileCount = fileCount
                        incomingTotalBytes = totalBytes
                        incomingApprovalCallback = { accepted ->
                            isApproved = accepted
                            incomingApprovalPending = false
                            approvalSignal.countDown()
                        }
                        incomingApprovalPending = true
                    }

                    approvalSignal.await()

                    if (!isApproved) {
                        val resp = JSONObject().apply {
                            put("status", "REJECTED")
                            put("reason", "Transfer declined by receiver.")
                        }
                        writer.println(resp.toString())
                        clientSocket.close()
                        continue
                    }

                    // Approved: Send ACCEPTED
                    writer.println(JSONObject().apply { put("status", "ACCEPTED") }.toString())

                    withContext(Dispatchers.Main) {
                        currentMode = TransferScreenMode.TRANSFER_ACTIVE
                        isSenderSession = false
                        transferStatus = TransferStatus.IN_PROGRESS
                        totalBytesToTransfer = totalBytes
                        transferredBytes = 0L
                    }

                    val inStream = clientSocket.getInputStream()
                    val buffer = ByteArray(64 * 1024)
                    var lastTime = System.currentTimeMillis()
                    var bytesSinceSample = 0L

                    // Read files loop
                    while (isActive && !TransferEngine.isCancelled.get()) {
                        val cmdLine = reader.readLine() ?: break
                        if (cmdLine.contains("ALL_COMPLETE")) {
                            break
                        }
                        val cmdObj = JSONObject(cmdLine)
                        if (cmdObj.optString("cmd") == "FILE_START") {
                            val fName = cmdObj.getString("name")
                            val fSize = cmdObj.getLong("size")

                            withContext(Dispatchers.Main) {
                                currentFileName = fName
                            }

                            val targetFile = TransferEngine.resolveUniqueFile(receivedDir, fName)
                            val partFile = File(receivedDir, "${targetFile.name}.part")

                            FileOutputStream(partFile).use { fos ->
                                var fileRemaining = fSize
                                while (fileRemaining > 0 && isActive && !TransferEngine.isCancelled.get()) {
                                    val toRead = minOf(buffer.size.toLong(), fileRemaining).toInt()
                                    val read = inStream.read(buffer, 0, toRead)
                                    if (read == -1) break
                                    fos.write(buffer, 0, read)
                                    fileRemaining -= read
                                    transferredBytes += read
                                    bytesSinceSample += read

                                    val now = System.currentTimeMillis()
                                    val elapsed = now - lastTime
                                    if (elapsed >= 500) {
                                        currentSpeedBps = (bytesSinceSample * 1000L) / elapsed
                                        bytesSinceSample = 0L
                                        lastTime = now
                                    }
                                }
                            }

                            // Atomic rename from .part to final file
                            if (partFile.exists() && partFile.length() == fSize) {
                                partFile.renameTo(targetFile)
                                writer.println("FILE_ACK:${fName}")
                            } else {
                                partFile.delete()
                                writer.println("FILE_ERROR:${fName}")
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        transferStatus = TransferStatus.COMPLETED
                        transferStatusMessage = "All $fileCount files received successfully into GMK Received!"
                        refreshReceivedFiles()
                    }
                    clientSocket.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    transferStatusMessage = "Server stopped: ${e.message}"
                }
            } finally {
                serverSocket?.close()
            }
        }
    }

    BackHandler {
        when (currentMode) {
            TransferScreenMode.MAIN -> onBack()
            TransferScreenMode.SELECT_FILES -> currentMode = TransferScreenMode.MAIN
            TransferScreenMode.CONNECT_PEER -> currentMode = TransferScreenMode.SELECT_FILES
            TransferScreenMode.TRANSFER_ACTIVE -> {
                if (transferStatus == TransferStatus.COMPLETED || transferStatus == TransferStatus.FAILED || transferStatus == TransferStatus.CANCELLED) {
                    currentMode = TransferScreenMode.MAIN
                } else {
                    TransferEngine.isCancelled.set(true)
                    currentMode = TransferScreenMode.MAIN
                }
            }
            TransferScreenMode.RECEIVE_WAITING -> {
                TransferEngine.isCancelled.set(true)
                try {
                    activeServerSocket?.close()
                } catch (_: Exception) {}
                receiverServerJob?.cancel()
                currentMode = TransferScreenMode.MAIN
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentMode) {
                            TransferScreenMode.MAIN -> "Send & Receive"
                            TransferScreenMode.SELECT_FILES -> "Select Files to Send"
                            TransferScreenMode.CONNECT_PEER -> "Connect to Receiver"
                            TransferScreenMode.TRANSFER_ACTIVE -> if (isSenderSession) "Sending Files..." else "Receiving Files..."
                            TransferScreenMode.RECEIVE_WAITING -> "Waiting for Sender..."
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentMode) {
                            TransferScreenMode.MAIN -> onBack()
                            TransferScreenMode.SELECT_FILES -> currentMode = TransferScreenMode.MAIN
                            TransferScreenMode.CONNECT_PEER -> currentMode = TransferScreenMode.SELECT_FILES
                            TransferScreenMode.TRANSFER_ACTIVE -> {
                                TransferEngine.isCancelled.set(true)
                                currentMode = TransferScreenMode.MAIN
                            }
                            TransferScreenMode.RECEIVE_WAITING -> {
                                receiverServerJob?.cancel()
                                currentMode = TransferScreenMode.MAIN
                            }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentMode == TransferScreenMode.MAIN) {
                        IconButton(onClick = { refreshReceivedFiles() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("send_receive_screen_container")
        ) {
            when (currentMode) {
                TransferScreenMode.MAIN -> {
                    MainShareMeDashboard(
                        localIp = localIp,
                        receivedFiles = receivedFilesList,
                        onSendClick = {
                            selectedFiles.clear()
                            currentMode = TransferScreenMode.SELECT_FILES
                        },
                        onReceiveClick = {
                            currentMode = TransferScreenMode.RECEIVE_WAITING
                            startReceiverServer()
                        },
                        onOpenFile = onOpenFile,
                        onShareFile = { file ->
                            val uri = Uri.fromFile(file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        },
                        onDeleteFile = { itemToDelete = it },
                        onRenameFile = {
                            itemToRename = it
                            newRenameInput = it.name
                        }
                    )
                }

                TransferScreenMode.SELECT_FILES -> {
                    SendFilePickerScreen(
                        allFiles = allDeviceFiles,
                        selectedFiles = selectedFiles,
                        selectedCategoryIndex = selectedCategoryIndex,
                        onCategorySelect = { selectedCategoryIndex = it },
                        onToggleFile = { file ->
                            if (selectedFiles.contains(file)) selectedFiles.remove(file) else selectedFiles.add(file)
                        },
                        onSelectAll = { files ->
                            selectedFiles.clear()
                            selectedFiles.addAll(files)
                        },
                        onClearAll = { selectedFiles.clear() },
                        onNext = {
                            currentMode = TransferScreenMode.CONNECT_PEER
                        }
                    )
                }

                TransferScreenMode.CONNECT_PEER -> {
                    ConnectPeerScreen(
                        localIp = localIp,
                        targetIp = targetIpInput,
                        onTargetIpChange = { targetIpInput = it },
                        targetPort = targetPortInput,
                        onTargetPortChange = { targetPortInput = it },
                        pin = pairingPin,
                        selectedFilesCount = selectedFiles.size,
                        selectedTotalBytes = selectedFiles.sumOf { it.length() },
                        onStartTransfer = {
                            val port = targetPortInput.toIntOrNull() ?: TransferEngine.DEFAULT_PORT
                            currentMode = TransferScreenMode.TRANSFER_ACTIVE
                            isSenderSession = true
                            transferStatus = TransferStatus.CONNECTING
                            totalBytesToTransfer = selectedFiles.sumOf { it.length() }
                            transferredBytes = 0L

                            scope.launch {
                                val result = TransferEngine.sendFiles(
                                    serverIp = targetIpInput.trim(),
                                    port = port,
                                    pin = pairingPin,
                                    senderDeviceName = Build.MODEL ?: "GMK Sender Device",
                                    files = selectedFiles.toList(),
                                    onProgress = { tBytes, total, speed, fName, fProg ->
                                        transferredBytes = tBytes
                                        totalBytesToTransfer = total
                                        currentSpeedBps = speed
                                        currentFileName = fName
                                    },
                                    onStatusChange = { st, msg ->
                                        transferStatus = st
                                        transferStatusMessage = msg
                                    }
                                )
                                if (result.isSuccess) {
                                    refreshReceivedFiles()
                                }
                            }
                        }
                    )
                }

                TransferScreenMode.TRANSFER_ACTIVE -> {
                    ActiveTransferProgressView(
                        isSender = isSenderSession,
                        status = transferStatus,
                        statusMessage = transferStatusMessage,
                        currentFile = currentFileName,
                        transferredBytes = transferredBytes,
                        totalBytes = totalBytesToTransfer,
                        speedBps = currentSpeedBps,
                        isPaused = isPausedState,
                        onPauseResume = {
                            isPausedState = !isPausedState
                            TransferEngine.isPaused.set(isPausedState)
                        },
                        onCancel = {
                            TransferEngine.isCancelled.set(true)
                            transferStatus = TransferStatus.CANCELLED
                            transferStatusMessage = "Transfer cancelled by user."
                        },
                        onDone = {
                            currentMode = TransferScreenMode.MAIN
                            refreshReceivedFiles()
                        }
                    )
                }

                TransferScreenMode.RECEIVE_WAITING -> {
                    ReceiveWaitingView(
                        localIp = localIp,
                        port = TransferEngine.DEFAULT_PORT,
                        pin = pairingPin,
                        statusMessage = transferStatusMessage,
                        onCancel = {
                            TransferEngine.isCancelled.set(true)
                            try {
                                activeServerSocket?.close()
                            } catch (_: Exception) {}
                            receiverServerJob?.cancel()
                            currentMode = TransferScreenMode.MAIN
                        }
                    )
                }
            }
        }
    }

    // Incoming transfer authorization dialog
    if (incomingApprovalPending) {
        AlertDialog(
            onDismissRequest = {
                incomingApprovalCallback?.invoke(false)
            },
            icon = {
                Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Incoming Transfer Request", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Device \"$incomingSenderName\" wants to send you:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Files: $incomingFileCount items", fontWeight = FontWeight.Medium)
                            Text("Total Size: ${TransferEngine.formatBytes(incomingTotalBytes)}", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            val free = TransferEngine.getAvailableFreeSpace(TransferEngine.getReceivedDirectory())
                            Text(
                                "Available Free Space: ${TransferEngine.formatBytes(free)}",
                                color = if (free > incomingTotalBytes) Color(0xFF1E8E3E) else Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Do you want to accept and download these files to GMK Received?", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { incomingApprovalCallback?.invoke(true) }) {
                    Text("Accept Transfer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { incomingApprovalCallback?.invoke(false) }) {
                    Text("Decline")
                }
            }
        )
    }

    // Delete item dialog
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
            title = { Text("Delete Received File?") },
            text = { Text("Permanently delete \"${item.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val f = File(item.path)
                        if (f.exists()) f.delete()
                        itemToDelete = null
                        refreshReceivedFiles()
                        scope.launch { snackbarHostState.showSnackbar("File deleted") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Rename dialog
    if (itemToRename != null) {
        val item = itemToRename!!
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = newRenameInput,
                    onValueChange = { newRenameInput = it },
                    singleLine = true,
                    label = { Text("New file name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val src = File(item.path)
                    val dest = File(src.parentFile, newRenameInput.trim())
                    if (src.exists() && newRenameInput.isNotBlank()) {
                        src.renameTo(dest)
                        refreshReceivedFiles()
                        scope.launch { snackbarHostState.showSnackbar("File renamed") }
                    }
                    itemToRename = null
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * ShareMe style dashboard with 2 massive tactile action buttons: SEND & RECEIVE
 */
@Composable
fun MainShareMeDashboard(
    localIp: String,
    receivedFiles: List<MediaItem>,
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onOpenFile: (MediaItem) -> Unit,
    onShareFile: (File) -> Unit,
    onDeleteFile: (MediaItem) -> Unit,
    onRenameFile: (MediaItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Big ShareMe Two-Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SEND CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(164.dp)
                        .clickable { onSendClick() }
                        .testTag("send_action_card"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0D47A1), // Deep Royal Blue
                                        Color(0xFF1976D2)  // Vibrant Royal Blue
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "SEND",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Share photos, videos, music, files & archives",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }

                // RECEIVE CARD
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(164.dp)
                        .clickable { onReceiveClick() }
                        .testTag("receive_action_card"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1565C0), // Royal Blue
                                        Color(0xFF1E88E5)  // Bright Royal Blue
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = "Receive",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "RECEIVE",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Direct Wi-Fi receiver engine, zero cloud upload",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Connection Info Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Direct Local Wi-Fi Protocol", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Device IP: $localIp  •  Port: ${TransferEngine.DEFAULT_PORT}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No internet or mobile data required. Streaming 64-bit engine supports up to 1TB+ files.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Received Files Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Received Files (${receivedFiles.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Saved to Downloads/GMK_Received",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (receivedFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No received files yet", color = Color.Gray, fontSize = 14.sp)
                        Text("Tap RECEIVE to wait for incoming transfers from other devices", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(receivedFiles) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(item) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val icon = when (item.mediaType) {
                                    MediaType.VIDEO -> Icons.Default.Movie
                                    MediaType.AUDIO -> Icons.Default.MusicNote
                                    MediaType.IMAGE -> Icons.Default.Image
                                    MediaType.ARCHIVE -> Icons.Default.Archive
                                    MediaType.DOCUMENT -> Icons.Default.Description
                                    else -> Icons.Default.Folder
                                }
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${TransferEngine.formatBytes(item.size)} • ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.lastModified))}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(onClick = { onShareFile(File(item.path)) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        }

                        IconButton(onClick = { onDeleteFile(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Categorized Send File Picker Screen
 */
@Composable
fun SendFilePickerScreen(
    allFiles: List<MediaItem>,
    selectedFiles: MutableList<File>,
    selectedCategoryIndex: Int,
    onCategorySelect: (Int) -> Unit,
    onToggleFile: (File) -> Unit,
    onSelectAll: (List<File>) -> Unit,
    onClearAll: () -> Unit,
    onNext: () -> Unit
) {
    val categories = listOf("All", "Videos", "Music", "Archives", "Documents", "Photos")

    val filtered = remember(allFiles, selectedCategoryIndex) {
        when (selectedCategoryIndex) {
            1 -> allFiles.filter { it.mediaType == MediaType.VIDEO }
            2 -> allFiles.filter { it.mediaType == MediaType.AUDIO }
            3 -> allFiles.filter { it.mediaType == MediaType.ARCHIVE }
            4 -> allFiles.filter { it.mediaType == MediaType.DOCUMENT }
            5 -> allFiles.filter { it.mediaType == MediaType.IMAGE }
            else -> allFiles
        }
    }

    val selectedBytes = remember(selectedFiles.size) {
        selectedFiles.sumOf { it.length() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            edgePadding = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedCategoryIndex == idx,
                    onClick = { onCategorySelect(idx) },
                    text = { Text(title) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filtered.size} items", fontSize = 12.sp, color = Color.Gray)
            Row {
                TextButton(onClick = {
                    val filesToSelect = filtered.map { File(it.path) }
                    onSelectAll(filesToSelect)
                }) {
                    Text("Select All", fontSize = 12.sp)
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered) { item ->
                val f = File(item.path)
                val isSelected = selectedFiles.any { it.absolutePath == f.absolutePath }
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFile(f) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleFile(f) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(TransferEngine.formatBytes(item.size), fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${selectedFiles.size} items selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Total: ${TransferEngine.formatBytes(selectedBytes)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onNext,
                    enabled = selectedFiles.isNotEmpty(),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("send_next_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Next: Find Receiver")
                }
            }
        }
    }
}

/**
 * Connect to Peer Screen
 */
@Composable
fun ConnectPeerScreen(
    localIp: String,
    targetIp: String,
    onTargetIpChange: (String) -> Unit,
    targetPort: String,
    onTargetPortChange: (String) -> Unit,
    pin: String,
    selectedFilesCount: Int,
    selectedTotalBytes: Long,
    onStartTransfer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
        }

        Text("Connect to Receiver Device", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Both devices must be connected to the same Wi-Fi network or Mobile Hotspot.",
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ready to Send: $selectedFilesCount files", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Total Batch Size: ${TransferEngine.formatBytes(selectedTotalBytes)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text("Your Local IP: $localIp", fontSize = 12.sp, color = Color.Gray)
            }
        }

        OutlinedTextField(
            value = targetIp,
            onValueChange = onTargetIpChange,
            label = { Text("Receiver Device IP Address") },
            placeholder = { Text("e.g. 192.168.1.105") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = targetPort,
                onValueChange = onTargetPortChange,
                label = { Text("Port") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = pin,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pairing PIN") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartTransfer,
            enabled = targetIp.isNotBlank(),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_transfer_button")
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send ${selectedFilesCount} Files (${TransferEngine.formatBytes(selectedTotalBytes)})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Receiver Waiting Screen
 */
@Composable
fun ReceiveWaitingView(
    localIp: String,
    port: Int,
    pin: String,
    statusMessage: String,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.CallReceived,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text("Ready to Receive", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(
            "Waiting for sender device to connect...",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("RECEIVER DETAILS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("IP Address: $localIp", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text("Port: $port", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("Pairing PIN: $pin", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(statusMessage, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Stop Receiving")
        }
    }
}

/**
 * Real-time Active Transfer Progress View (Sender & Receiver)
 */
@Composable
fun ActiveTransferProgressView(
    isSender: Boolean,
    status: TransferStatus,
    statusMessage: String,
    currentFile: String,
    transferredBytes: Long,
    totalBytes: Long,
    speedBps: Long,
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val progress = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val remainingBytes = (totalBytes - transferredBytes).coerceAtLeast(0L)
    val eta = TransferEngine.formatEta(remainingBytes, speedBps)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (isSender) "Sending to Receiver" else "Receiving from Sender",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(statusMessage, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(32.dp))

            // Large Circular Progress
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 10.dp,
                    color = when (status) {
                        TransferStatus.COMPLETED -> Color(0xFF1E8E3E)
                        TransferStatus.FAILED -> Color.Red
                        TransferStatus.CANCELLED -> Color.Gray
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    )
                    Text(
                        TransferEngine.formatSpeed(speedBps),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current File:", fontSize = 11.sp, color = Color.Gray)
                        Text("ETA: $eta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        if (currentFile.isNotBlank()) currentFile else "Preparing stream...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${TransferEngine.formatBytes(transferredBytes)} / ${TransferEngine.formatBytes(totalBytes)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            TransferEngine.formatSpeed(speedBps),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (status == TransferStatus.COMPLETED || status == TransferStatus.FAILED || status == TransferStatus.CANCELLED) {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Finish & View Files")
                }
            } else {
                OutlinedButton(
                    onClick = onPauseResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPaused) "Resume" else "Pause")
                }

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}
