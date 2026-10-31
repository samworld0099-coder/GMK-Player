package com.example.browser

import android.content.Context
import java.io.File

/**
 * Compatibility bridge forwarding to the high-performance DownloadAccelerationEngine.
 */
object DownloadManagerHelper {

    fun getDownloadDirectory(): File {
        val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, "GMK_Downloads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAllTasks(context: Context): List<DownloadTask> {
        return DownloadAccelerationEngine.getInstance(context).getAllTasks()
    }

    fun pauseTask(context: Context, id: String) {
        DownloadAccelerationEngine.getInstance(context).pauseDownload(id)
    }

    fun resumeTask(context: Context, id: String, userAgent: String? = null) {
        DownloadAccelerationEngine.getInstance(context).resumeDownload(id, userAgent)
    }

    fun cancelTask(context: Context, id: String) {
        DownloadAccelerationEngine.getInstance(context).cancelDownload(id)
    }

    fun retryTask(context: Context, id: String, userAgent: String? = null) {
        DownloadAccelerationEngine.getInstance(context).retryDownload(id, userAgent)
    }

    fun deleteTask(context: Context, id: String, deleteFromDisk: Boolean) {
        DownloadAccelerationEngine.getInstance(context).deleteDownload(id, deleteFromDisk)
    }

    /**
     * Backward-compatible delegation to start download via the accelerated engine.
     */
    fun enqueue(
        context: Context,
        url: String,
        fileName: String,
        userAgent: String?
    ): DownloadTask {
        return DownloadAccelerationEngine.getInstance(context).enqueueDownload(
            url = url,
            suggestedFileName = fileName,
            userAgent = userAgent
        )
    }
}
