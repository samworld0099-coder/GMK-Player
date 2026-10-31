package com.example.filemanager

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoThumbnailLoader {

    private val cache = object : LruCache<String, Bitmap>(100) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    suspend fun loadThumbnail(context: Context, item: MediaItem, width: Int = 320, height: Int = 240): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${item.uri}_${item.lastModified}_${width}x$height"
        cache.get(cacheKey)?.let { return@withContext it }

        var bitmap: Bitmap? = null

        // 1. Try modern ContentResolver API for content URIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && item.uri.scheme == "content") {
            try {
                bitmap = context.contentResolver.loadThumbnail(item.uri, Size(width, height), null)
            } catch (_: Throwable) {}
        }

        // 2. Fallback to physical path if available
        if (bitmap == null && item.path.isNotEmpty()) {
            val file = File(item.path)
            if (file.exists()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        bitmap = ThumbnailUtils.createVideoThumbnail(file, Size(width, height), null)
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap = ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
                    }
                } catch (_: Throwable) {}
            }
        }

        // 3. Fallback to MediaMetadataRetriever
        if (bitmap == null) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                if (item.uri.scheme == "content") {
                    retriever.setDataSource(context, item.uri)
                } else if (item.path.isNotEmpty()) {
                    retriever.setDataSource(item.path)
                }
                bitmap = retriever.getFrameAtTime(1_000_000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
            } catch (_: Throwable) {}
        }

        if (bitmap != null) {
            cache.put(cacheKey, bitmap)
        }
        bitmap
    }
}
