package com.example.browser

enum class DownloadTaskStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val id: String,
    val url: String,
    var fileName: String,
    var destinationPath: String,
    var totalBytes: Long = 0L,
    var downloadedBytes: Long = 0L,
    var speedBps: Long = 0L,
    var status: DownloadTaskStatus = DownloadTaskStatus.PENDING,
    var errorMessage: String? = null
)

data class DetectedMediaSource(
    val url: String,
    val title: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val resolution: String? = null
)
