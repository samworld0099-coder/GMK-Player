package com.example.data.model

import android.net.Uri

enum class MediaType {
    VIDEO,
    AUDIO,
    ARCHIVE,
    IMAGE,
    DOCUMENT,
    FOLDER,
    OTHER
}

enum class VideoOrigin(val displayName: String) {
    ALL("All"),
    WHATSAPP("WhatsApp"),
    GMK_BROWSER("GMK Browser"),
    CAMERA("Camera"),
    DOWNLOADS("Downloads"),
    TELEGRAM("Telegram"),
    BLUETOOTH("Bluetooth"),
    RECEIVED("Received Files"),
    OTHER("Other")
}

data class MediaItem(
    val uri: Uri,
    val name: String,
    val path: String,
    val size: Long = 0L,
    val mimeType: String? = null,
    val durationMs: Long = 0L,
    val resolution: String? = null,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val mediaType: MediaType = MediaType.OTHER,
    val extension: String = "",
    val isArchive: Boolean = false,
    val childCount: Int = 0,
    val origin: VideoOrigin = VideoOrigin.OTHER
) {
    val formattedSize: String
        get() {
            if (isDirectory) return if (childCount > 0) "$childCount items" else "Folder"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}

data class StorageInfo(
    val title: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val path: String
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0f
    val formattedTotal: String get() = formatBytes(totalBytes)
    val formattedUsed: String get() = formatBytes(usedBytes)
    val formattedFree: String get() = formatBytes(freeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) String.format("%.1f GB", gb)
            else String.format("%.0f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
