package com.example.archive

import android.content.Context
import android.net.Uri
import com.example.data.model.ArchiveEntryItem
import com.example.data.model.ArchiveExtractionProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile as CommonsZipFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile as StandardZipFile
import java.util.zip.ZipOutputStream

object ArchiveManager {

    private const val BUFFER_SIZE = 32 * 1024
    private const val MAX_UNCOMPRESSED_RATIO = 250 // Zip bomb heuristic threshold
    private const val MAX_ENTRY_SIZE_BYTES = 4L * 1024 * 1024 * 1024 // 4GB max entry safety limit

    enum class ArchiveFormat {
        ZIP, SEVEN_Z, TAR, TAR_GZ, GZIP, UNKNOWN
    }

    fun detectFormat(file: File): ArchiveFormat {
        val name = file.name.lowercase(Locale.ROOT)
        return when {
            name.endsWith(".zip") -> ArchiveFormat.ZIP
            name.endsWith(".7z") -> ArchiveFormat.SEVEN_Z
            name.endsWith(".tar.gz") || name.endsWith(".tgz") -> ArchiveFormat.TAR_GZ
            name.endsWith(".tar") -> ArchiveFormat.TAR
            name.endsWith(".gz") -> ArchiveFormat.GZIP
            else -> ArchiveFormat.UNKNOWN
        }
    }

    /**
     * Reads all archive entries without extracting, parsing them into uniform ArchiveEntryItem list.
     */
    suspend fun listEntries(file: File): Result<List<ArchiveEntryItem>> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext Result.failure(IOException("Archive file does not exist: ${file.path}"))
        if (file.length() == 0L) return@withContext Result.failure(IOException("Archive file is empty (0 bytes)"))

        val format = detectFormat(file)
        try {
            when (format) {
                ArchiveFormat.ZIP -> listZipEntries(file)
                ArchiveFormat.SEVEN_Z -> listSevenZEntries(file)
                ArchiveFormat.TAR, ArchiveFormat.TAR_GZ -> listTarEntries(file, isGz = format == ArchiveFormat.TAR_GZ)
                ArchiveFormat.GZIP -> {
                    // Single gz file
                    val innerName = file.nameWithoutExtension
                    Result.success(
                        listOf(
                            ArchiveEntryItem(
                                path = innerName,
                                name = innerName,
                                isDirectory = false,
                                size = file.length() * 2,
                                compressedSize = file.length(),
                                method = "GZIP"
                            )
                        )
                    )
                }
                ArchiveFormat.UNKNOWN -> {
                    // Try parsing as ZIP first, then TAR
                    val zipAttempt = listZipEntries(file)
                    if (zipAttempt.isSuccess) zipAttempt else listTarEntries(file, isGz = false)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun listZipEntries(file: File): Result<List<ArchiveEntryItem>> {
        // Try Apache Commons Compress ZipFile first (supports Zip64, UTF-8 extra fields, comments)
        try {
            CommonsZipFile(file, "UTF-8", true).use { zipFile ->
                val entries = mutableListOf<ArchiveEntryItem>()
                val enumeration = zipFile.entries
                while (enumeration.hasMoreElements()) {
                    val entry: ZipArchiveEntry = enumeration.nextElement()
                    val normalizedPath = entry.name.replace('\\', '/').trimEnd('/')
                    val name = normalizedPath.substringAfterLast('/')
                    entries.add(
                        ArchiveEntryItem(
                            path = normalizedPath,
                            name = if (name.isEmpty()) normalizedPath else name,
                            isDirectory = entry.isDirectory,
                            size = entry.size.coerceAtLeast(0L),
                            compressedSize = entry.compressedSize.coerceAtLeast(0L),
                            lastModified = entry.time,
                            method = entry.method.let { if (it == ZipEntry.DEFLATED) "Deflate" else "Stored" },
                            comment = entry.comment
                        )
                    )
                }
                return Result.success(entries)
            }
        } catch (e: Exception) {
            // Fallback to java.util.zip.ZipFile
            try {
                StandardZipFile(file).use { stdZip ->
                    val entries = mutableListOf<ArchiveEntryItem>()
                    val enumeration = stdZip.entries()
                    while (enumeration.hasMoreElements()) {
                        val entry = enumeration.nextElement()
                        val normalizedPath = entry.name.replace('\\', '/').trimEnd('/')
                        val name = normalizedPath.substringAfterLast('/')
                        entries.add(
                            ArchiveEntryItem(
                                path = normalizedPath,
                                name = if (name.isEmpty()) normalizedPath else name,
                                isDirectory = entry.isDirectory,
                                size = entry.size.coerceAtLeast(0L),
                                compressedSize = entry.compressedSize.coerceAtLeast(0L),
                                lastModified = entry.time,
                                method = if (entry.method == ZipEntry.DEFLATED) "Deflate" else "Stored",
                                comment = entry.comment
                            )
                        )
                    }
                    return Result.success(entries)
                }
            } catch (fallbackError: Exception) {
                return Result.failure(
                    IOException(
                        "Unable to open ZIP archive. It may be password-protected, encrypted, or corrupted: ${fallbackError.message}",
                        e
                    )
                )
            }
        }
    }

    private fun listSevenZEntries(file: File): Result<List<ArchiveEntryItem>> {
        try {
            SevenZFile(file).use { sevenZ ->
                val entries = mutableListOf<ArchiveEntryItem>()
                for (entry in sevenZ.entries) {
                    val normalizedPath = entry.name.replace('\\', '/').trimEnd('/')
                    val name = normalizedPath.substringAfterLast('/')
                    entries.add(
                        ArchiveEntryItem(
                            path = normalizedPath,
                            name = if (name.isEmpty()) normalizedPath else name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            compressedSize = entry.size,
                            lastModified = entry.lastModifiedDate?.time ?: 0L,
                            method = "7z LZMA/LZMA2",
                            isEncrypted = false
                        )
                    )
                }
                return Result.success(entries)
            }
        } catch (e: Exception) {
            return Result.failure(IOException("Failed to read 7z archive: ${e.message}", e))
        }
    }

    private fun listTarEntries(file: File, isGz: Boolean): Result<List<ArchiveEntryItem>> {
        try {
            var rawStream: InputStream = BufferedInputStream(FileInputStream(file))
            if (isGz) {
                rawStream = GzipCompressorInputStream(rawStream)
            }
            TarArchiveInputStream(rawStream).use { tarIn ->
                val entries = mutableListOf<ArchiveEntryItem>()
                var entry: TarArchiveEntry? = tarIn.nextEntry
                while (entry != null) {
                    val normalizedPath = entry.name.replace('\\', '/').trimEnd('/')
                    val name = normalizedPath.substringAfterLast('/')
                    entries.add(
                        ArchiveEntryItem(
                            path = normalizedPath,
                            name = if (name.isEmpty()) normalizedPath else name,
                            isDirectory = entry.isDirectory,
                            size = entry.size,
                            compressedSize = entry.size,
                            lastModified = entry.modTime.time,
                            method = if (isGz) "TAR + GZIP" else "TAR"
                        )
                    )
                    entry = tarIn.nextEntry
                }
                return Result.success(entries)
            }
        } catch (e: Exception) {
            return Result.failure(IOException("Failed to read TAR archive: ${e.message}", e))
        }
    }

    /**
     * Extracts all or selected paths from an archive to destinationDir safely.
     * Prevents path traversal vulnerabilities ("Zip Slip").
     */
    suspend fun extract(
        archiveFile: File,
        destinationDir: File,
        selectedPaths: Set<String>? = null, // null means extract all
        onProgress: (ArchiveExtractionProgress) -> Unit,
        isCancelled: () -> Boolean = { false }
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!archiveFile.exists()) return@withContext Result.failure(IOException("Archive file not found"))
        if (!destinationDir.exists()) destinationDir.mkdirs()

        val destCanonicalPath = destinationDir.canonicalPath
        val format = detectFormat(archiveFile)

        var extractedCount = 0
        var bytesExtracted = 0L

        try {
            when (format) {
                ArchiveFormat.ZIP -> {
                    StandardZipFile(archiveFile).use { zip ->
                        val totalEntries = if (selectedPaths == null) zip.size() else selectedPaths.size
                        val enumeration = zip.entries()

                        while (enumeration.hasMoreElements()) {
                            if (isCancelled()) {
                                return@withContext Result.failure(IOException("Extraction cancelled by user"))
                            }
                            val entry = enumeration.nextElement()
                            val normalized = entry.name.replace('\\', '/').trimEnd('/')

                            if (selectedPaths != null) {
                                val match = selectedPaths.any { path ->
                                    normalized == path || normalized.startsWith("$path/")
                                }
                                if (!match) continue
                            }

                            // Security check: Path Traversal Guard
                            val targetFile = File(destinationDir, normalized)
                            val targetCanonical = targetFile.canonicalPath
                            if (!targetCanonical.startsWith(destCanonicalPath + File.separator) && targetCanonical != destCanonicalPath) {
                                return@withContext Result.failure(
                                    SecurityException("Malicious archive entry detected attempting path traversal: ${entry.name}")
                                )
                            }

                            if (entry.isDirectory) {
                                targetFile.mkdirs()
                            } else {
                                targetFile.parentFile?.mkdirs()
                                // Zip bomb heuristics check
                                if (entry.size > MAX_ENTRY_SIZE_BYTES) {
                                    return@withContext Result.failure(
                                        SecurityException("Entry size exceeds 4GB limit, possible decompression bomb: ${entry.name}")
                                    )
                                }

                                zip.getInputStream(entry).use { inStream ->
                                    FileOutputStream(targetFile).use { outStream ->
                                        val buffer = ByteArray(BUFFER_SIZE)
                                        var len: Int
                                        var entryBytes = 0L
                                        while (inStream.read(buffer).also { len = it } != -1) {
                                            if (isCancelled()) {
                                                return@withContext Result.failure(IOException("Extraction cancelled by user"))
                                            }
                                            outStream.write(buffer, 0, len)
                                            entryBytes += len
                                            bytesExtracted += len
                                        }
                                    }
                                }
                                extractedCount++
                            }

                            onProgress(
                                ArchiveExtractionProgress(
                                    currentFileName = targetFile.name,
                                    entriesProcessed = extractedCount,
                                    totalEntries = totalEntries,
                                    bytesExtracted = bytesExtracted,
                                    isFinished = false
                                )
                            )
                        }
                    }
                }
                ArchiveFormat.SEVEN_Z -> {
                    SevenZFile(archiveFile).use { sevenZ ->
                        val allEntries = sevenZ.entries.toList()
                        val totalEntries = if (selectedPaths == null) allEntries.size else selectedPaths.size

                        for (entry in allEntries) {
                            if (isCancelled()) return@withContext Result.failure(IOException("Extraction cancelled"))
                            val normalized = entry.name.replace('\\', '/').trimEnd('/')

                            if (selectedPaths != null) {
                                val match = selectedPaths.any { path ->
                                    normalized == path || normalized.startsWith("$path/")
                                }
                                if (!match) continue
                            }

                            val targetFile = File(destinationDir, normalized)
                            val targetCanonical = targetFile.canonicalPath
                            if (!targetCanonical.startsWith(destCanonicalPath + File.separator) && targetCanonical != destCanonicalPath) {
                                return@withContext Result.failure(
                                    SecurityException("Malicious 7z entry attempting path traversal: ${entry.name}")
                                )
                            }

                            if (entry.isDirectory) {
                                targetFile.mkdirs()
                            } else {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { outStream ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var count: Int
                                    while (sevenZ.read(buffer).also { count = it } > 0) {
                                        if (isCancelled()) return@withContext Result.failure(IOException("Extraction cancelled"))
                                        outStream.write(buffer, 0, count)
                                        bytesExtracted += count
                                    }
                                }
                                extractedCount++
                            }

                            onProgress(
                                ArchiveExtractionProgress(
                                    currentFileName = targetFile.name,
                                    entriesProcessed = extractedCount,
                                    totalEntries = totalEntries,
                                    bytesExtracted = bytesExtracted,
                                    isFinished = false
                                )
                            )
                        }
                    }
                }
                ArchiveFormat.TAR, ArchiveFormat.TAR_GZ -> {
                    var inStream: InputStream = BufferedInputStream(FileInputStream(archiveFile))
                    if (format == ArchiveFormat.TAR_GZ) {
                        inStream = GzipCompressorInputStream(inStream)
                    }
                    TarArchiveInputStream(inStream).use { tarIn ->
                        var entry = tarIn.nextEntry
                        while (entry != null) {
                            if (isCancelled()) return@withContext Result.failure(IOException("Extraction cancelled"))
                            val normalized = entry.name.replace('\\', '/').trimEnd('/')

                            if (selectedPaths != null) {
                                val match = selectedPaths.any { path ->
                                    normalized == path || normalized.startsWith("$path/")
                                }
                                if (!match) {
                                    entry = tarIn.nextEntry
                                    continue
                                }
                            }

                            val targetFile = File(destinationDir, normalized)
                            val targetCanonical = targetFile.canonicalPath
                            if (!targetCanonical.startsWith(destCanonicalPath + File.separator) && targetCanonical != destCanonicalPath) {
                                return@withContext Result.failure(
                                    SecurityException("Malicious TAR entry attempting path traversal: ${entry.name}")
                                )
                            }

                            if (entry.isDirectory) {
                                targetFile.mkdirs()
                            } else {
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { outStream ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var count: Int
                                    while (tarIn.read(buffer).also { count = it } != -1) {
                                        if (isCancelled()) return@withContext Result.failure(IOException("Extraction cancelled"))
                                        outStream.write(buffer, 0, count)
                                        bytesExtracted += count
                                    }
                                }
                                extractedCount++
                            }

                            onProgress(
                                ArchiveExtractionProgress(
                                    currentFileName = targetFile.name,
                                    entriesProcessed = extractedCount,
                                    bytesExtracted = bytesExtracted,
                                    isFinished = false
                                )
                            )
                            entry = tarIn.nextEntry
                        }
                    }
                }
                else -> {
                    return@withContext Result.failure(IOException("Unsupported archive format for extraction: ${archiveFile.name}"))
                }
            }

            onProgress(
                ArchiveExtractionProgress(
                    currentFileName = "Completed",
                    entriesProcessed = extractedCount,
                    totalEntries = extractedCount,
                    bytesExtracted = bytesExtracted,
                    isFinished = true
                )
            )
            Result.success(extractedCount)
        } catch (e: Exception) {
            onProgress(
                ArchiveExtractionProgress(
                    currentFileName = "Failed",
                    isFinished = true,
                    error = e.message ?: "Extraction failed"
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Creates a new ZIP archive from the given source files or directories.
     */
    suspend fun createZipArchive(
        sources: List<File>,
        outputZipFile: File,
        compressionLevel: Int = 6, // 0 (STORED) to 9 (BEST_COMPRESSION)
        onProgress: (currentFile: String, filesCompressed: Int) -> Unit,
        isCancelled: () -> Boolean = { false }
    ): Result<File> = withContext(Dispatchers.IO) {
        if (sources.isEmpty()) return@withContext Result.failure(IllegalArgumentException("No files selected for archive"))
        outputZipFile.parentFile?.mkdirs()

        try {
            var count = 0
            FileOutputStream(outputZipFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        zos.setLevel(compressionLevel.coerceIn(0, 9))

                        fun addFile(file: File, basePath: String) {
                            if (isCancelled()) return
                            val entryPath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

                            if (file.isDirectory) {
                                val dirEntry = ZipEntry("$entryPath/")
                                dirEntry.time = file.lastModified()
                                zos.putNextEntry(dirEntry)
                                zos.closeEntry()

                                file.listFiles()?.forEach { child ->
                                    addFile(child, entryPath)
                                }
                            } else {
                                val fileEntry = ZipEntry(entryPath)
                                fileEntry.time = file.lastModified()
                                fileEntry.size = file.length()
                                zos.putNextEntry(fileEntry)

                                FileInputStream(file).use { fis ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var read: Int
                                    while (fis.read(buffer).also { read = it } != -1) {
                                        if (isCancelled()) return
                                        zos.write(buffer, 0, read)
                                    }
                                }
                                zos.closeEntry()
                                count++
                                onProgress(file.name, count)
                            }
                        }

                        for (source in sources) {
                            addFile(source, "")
                        }
                    }
                }
            }
            if (isCancelled()) {
                outputZipFile.delete()
                return@withContext Result.failure(IOException("Archive creation cancelled"))
            }
            Result.success(outputZipFile)
        } catch (e: Exception) {
            outputZipFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Creates a TAR archive from the given source files or directories.
     */
    suspend fun createTarArchive(
        sources: List<File>,
        outputTarFile: File,
        isGz: Boolean = false,
        onProgress: (currentFile: String, filesProcessed: Int) -> Unit,
        isCancelled: () -> Boolean = { false }
    ): Result<File> = withContext(Dispatchers.IO) {
        if (sources.isEmpty()) return@withContext Result.failure(IllegalArgumentException("No files selected"))
        outputTarFile.parentFile?.mkdirs()

        try {
            var count = 0
            var fos: java.io.OutputStream = FileOutputStream(outputTarFile)
            if (isGz) {
                fos = GzipCompressorOutputStream(fos)
            }
            TarArchiveOutputStream(BufferedOutputStream(fos)).use { tos ->
                tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)

                fun addTarFile(file: File, basePath: String) {
                    if (isCancelled()) return
                    val entryPath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
                    val entry = TarArchiveEntry(file, entryPath)
                    tos.putArchiveEntry(entry)

                    if (file.isFile) {
                        FileInputStream(file).use { fis ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int
                            while (fis.read(buffer).also { read = it } != -1) {
                                if (isCancelled()) return
                                tos.write(buffer, 0, read)
                            }
                        }
                        tos.closeArchiveEntry()
                        count++
                        onProgress(file.name, count)
                    } else if (file.isDirectory) {
                        tos.closeArchiveEntry()
                        file.listFiles()?.forEach { child ->
                            addTarFile(child, entryPath)
                        }
                    }
                }

                for (source in sources) {
                    addTarFile(source, "")
                }
            }
            if (isCancelled()) {
                outputTarFile.delete()
                return@withContext Result.failure(IOException("Tar creation cancelled"))
            }
            Result.success(outputTarFile)
        } catch (e: Exception) {
            outputTarFile.delete()
            Result.failure(e)
        }
    }
}
