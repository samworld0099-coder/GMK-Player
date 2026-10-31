package com.example.filemanager

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

enum class SortField { NAME, SIZE, DATE, TYPE }
enum class SortOrder { ASCENDING, DESCENDING }
enum class ConflictResolution { REPLACE, SKIP, RENAME }

object FileManagerHelper {

    fun getStorageVolumes(context: Context): List<StorageInfo> {
        val list = mutableListOf<StorageInfo>()
        try {
            val primary = Environment.getExternalStorageDirectory()
            if (primary != null && primary.exists()) {
                val stat = StatFs(primary.path)
                val total = stat.totalBytes
                val free = stat.availableBytes
                list.add(
                    StorageInfo(
                        title = "Internal Storage",
                        totalBytes = total,
                        freeBytes = free,
                        path = primary.absolutePath
                    )
                )
            }
        } catch (_: Exception) {}

        // Add App-specific External Storage
        try {
            val appExt = context.getExternalFilesDir(null)
            if (appExt != null && appExt.exists()) {
                val stat = StatFs(appExt.path)
                list.add(
                    StorageInfo(
                        title = "App Storage",
                        totalBytes = stat.totalBytes,
                        freeBytes = stat.availableBytes,
                        path = appExt.absolutePath
                    )
                )
            }
        } catch (_: Exception) {}

        return list
    }

    fun getQuickFolders(context: Context): List<MediaItem> {
        val folders = mutableListOf<MediaItem>()
        val base = Environment.getExternalStorageDirectory() ?: return folders

        val quickPaths = listOf(
            "Downloads" to Environment.DIRECTORY_DOWNLOADS,
            "Movies" to Environment.DIRECTORY_MOVIES,
            "Music" to Environment.DIRECTORY_MUSIC,
            "Pictures" to Environment.DIRECTORY_PICTURES,
            "Documents" to Environment.DIRECTORY_DOCUMENTS,
            "DCIM" to Environment.DIRECTORY_DCIM
        )

        for ((name, dirType) in quickPaths) {
            val file = Environment.getExternalStoragePublicDirectory(dirType)
            if (file != null && file.exists()) {
                folders.add(fileToMediaItem(file, overrideName = name))
            } else {
                val fallback = File(base, name)
                if (fallback.exists()) {
                    folders.add(fileToMediaItem(fallback, overrideName = name))
                }
            }
        }
        return folders
    }

    suspend fun listDirectory(
        directory: File,
        sortField: SortField = SortField.NAME,
        sortOrder: SortOrder = SortOrder.ASCENDING,
        showHidden: Boolean = false
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val files = directory.listFiles() ?: return@withContext emptyList()
        val items = files
            .filter { showHidden || !it.name.startsWith(".") }
            .map { fileToMediaItem(it) }

        sortItems(items, sortField, sortOrder)
    }

    fun sortItems(
        items: List<MediaItem>,
        sortField: SortField,
        sortOrder: SortOrder
    ): List<MediaItem> {
        val comparator = when (sortField) {
            SortField.NAME -> compareBy<MediaItem> { it.name.lowercase(Locale.ROOT) }
            SortField.SIZE -> compareBy { it.size }
            SortField.DATE -> compareBy { it.lastModified }
            SortField.TYPE -> compareBy<MediaItem> { it.mediaType.ordinal }.thenBy { it.extension }
        }

        val sorted = if (sortOrder == SortOrder.ASCENDING) {
            items.sortedWith(comparator)
        } else {
            items.sortedWith(comparator.reversed())
        }

        // Keep folders first
        val (dirs, files) = sorted.partition { it.isDirectory }
        return dirs + files
    }

    fun fileToMediaItem(file: File, overrideName: String? = null, extractMetadata: Boolean = false): MediaItem {
        val isDir = file.isDirectory
        val name = overrideName ?: file.name
        val extension = if (isDir) "" else file.extension.lowercase(Locale.ROOT)
        val mimeType = getMimeTypeFromExtension(extension)
        val mediaType = determineMediaType(extension, isDir)
        val isArchive = isArchiveExtension(extension)
        val childCount = if (isDir) (file.list()?.size ?: 0) else 0

        var durationMs = 0L
        var resolution: String? = null

        if (extractMetadata && file.canRead()) {
            if (mediaType == MediaType.VIDEO) {
                try {
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(file.absolutePath)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = durStr?.toLongOrNull() ?: 0L
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                        if (width != null && height != null) {
                            resolution = "${width}x${height}"
                        }
                    }
                } catch (_: Throwable) {}
            } else if (mediaType == MediaType.AUDIO) {
                try {
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(file.absolutePath)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durationMs = durStr?.toLongOrNull() ?: 0L
                    }
                } catch (_: Throwable) {}
            }
        }

        return MediaItem(
            uri = Uri.fromFile(file),
            name = name,
            path = file.absolutePath,
            size = if (isDir) 0L else file.length(),
            mimeType = mimeType,
            durationMs = durationMs,
            resolution = resolution,
            lastModified = file.lastModified(),
            isDirectory = isDir,
            mediaType = mediaType,
            extension = extension,
            isArchive = isArchive,
            childCount = childCount
        )
    }

    fun determineMediaType(extension: String, isDir: Boolean): MediaType {
        if (isDir) return MediaType.FOLDER
        return when (extension) {
            "mp4", "mkv", "webm", "avi", "mov", "ts", "m4v", "flv", "3gp", "wmv", "mpg", "mpeg" -> MediaType.VIDEO
            "mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma", "mid" -> MediaType.AUDIO
            "zip", "7z", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz", "rar", "iso" -> MediaType.ARCHIVE
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> MediaType.IMAGE
            "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "json", "xml", "md", "csv", "log" -> MediaType.DOCUMENT
            else -> MediaType.OTHER
        }
    }

    fun isArchiveExtension(extension: String): Boolean {
        return when (extension.lowercase(Locale.ROOT)) {
            "zip", "7z", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz", "rar", "iso" -> true
            else -> false
        }
    }

    fun getMimeTypeFromExtension(ext: String): String? {
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz", "tgz" -> "application/gzip"
            "bz2", "tbz2" -> "application/x-bzip2"
            "xz" -> "application/x-xz"
            "rar" -> "application/x-rar-compressed"
            "iso" -> "application/x-iso9660-image"
            else -> null
        }
    }

    // --- File Operations ---

    suspend fun createFolder(parent: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        val cleanName = folderName.trim()
        if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid folder name"))
        }
        val target = File(parent, cleanName)
        if (target.exists()) {
            return@withContext Result.failure(FileAlreadyExistsException(target, reason = "Folder already exists"))
        }
        val created = target.mkdirs()
        if (created) Result.success(target)
        else Result.failure(IOException("Failed to create folder. Ensure write permissions."))
    }

    suspend fun createTextFile(parent: File, fileName: String, initialContent: String = ""): Result<File> = withContext(Dispatchers.IO) {
        val cleanName = if (fileName.contains(".")) fileName.trim() else "${fileName.trim()}.txt"
        if (cleanName.contains("/") || cleanName.contains("\\")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid file name"))
        }
        val target = File(parent, cleanName)
        if (target.exists()) {
            return@withContext Result.failure(FileAlreadyExistsException(target, reason = "File already exists"))
        }
        try {
            target.writeText(initialContent)
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty() || cleanName.contains("/") || cleanName.contains("\\")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid name"))
        }
        val destination = File(file.parentFile, cleanName)
        if (destination.exists()) {
            return@withContext Result.failure(FileAlreadyExistsException(destination, reason = "Target name already exists"))
        }
        val success = file.renameTo(destination)
        if (success) Result.success(destination)
        else Result.failure(IOException("Rename failed. Destination may be read-only or in use."))
    }

    suspend fun deleteRecursively(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val success = file.deleteRecursively()
            if (success) Result.success(true)
            else Result.failure(IOException("Could not completely delete ${file.name}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copy(
        source: File,
        destinationDir: File,
        conflict: ConflictResolution = ConflictResolution.RENAME
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            var targetFile = File(destinationDir, source.name)

            if (targetFile.exists()) {
                when (conflict) {
                    ConflictResolution.SKIP -> return@withContext Result.success(targetFile)
                    ConflictResolution.REPLACE -> {
                        if (targetFile.isDirectory) targetFile.deleteRecursively()
                        else targetFile.delete()
                    }
                    ConflictResolution.RENAME -> {
                        var counter = 1
                        val baseName = source.nameWithoutExtension
                        val ext = if (source.extension.isNotEmpty()) ".${source.extension}" else ""
                        while (targetFile.exists()) {
                            targetFile = File(destinationDir, "$baseName ($counter)$ext")
                            counter++
                        }
                    }
                }
            }

            if (source.isDirectory) {
                source.copyRecursively(targetFile, overwrite = true)
            } else {
                FileInputStream(source).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun move(
        source: File,
        destinationDir: File,
        conflict: ConflictResolution = ConflictResolution.RENAME
    ): Result<File> = withContext(Dispatchers.IO) {
        val copyResult = copy(source, destinationDir, conflict)
        if (copyResult.isSuccess) {
            source.deleteRecursively()
            copyResult
        } else {
            copyResult
        }
    }
}
