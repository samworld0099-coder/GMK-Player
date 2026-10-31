package com.example.data.model

data class ArchiveEntryItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val compressedSize: Long = 0L,
    val lastModified: Long = 0L,
    val method: String? = null,
    val isEncrypted: Boolean = false,
    val comment: String? = null,
    val childCount: Int = 0
) {
    val formattedSize: String
        get() {
            if (isDirectory) return if (childCount > 0) "$childCount items" else "Folder"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$size B"
            }
        }

    val compressionRatio: String
        get() {
            if (isDirectory || size <= 0 || compressedSize <= 0) return "-"
            val ratio = (1.0 - (compressedSize.toDouble() / size.toDouble())) * 100.0
            return String.format("%.0f%%", ratio.coerceIn(0.0, 100.0))
        }
}

data class ArchiveExtractionProgress(
    val currentFileName: String = "",
    val entriesProcessed: Int = 0,
    val totalEntries: Int = 0,
    val bytesExtracted: Long = 0L,
    val totalBytes: Long = 0L,
    val isFinished: Boolean = false,
    val error: String? = null
) {
    val progressFraction: Float
        get() = when {
            totalEntries > 0 -> entriesProcessed.toFloat() / totalEntries
            totalBytes > 0 -> bytesExtracted.toFloat() / totalBytes
            else -> 0f
        }.coerceIn(0f, 1f)
}
