package com.example.filemanager

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.VideoOrigin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object MediaStoreHelper {

    val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi", "mov", "ts", "m4v", "flv", "3gp", "wmv", "mpg", "mpeg")
    val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma", "mid", "amr")

    private const val PREFS_CUSTOM_FOLDERS = "gmk_custom_folders"
    private const val KEY_FOLDER_PATHS = "folder_paths"

    fun getCustomFolders(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_CUSTOM_FOLDERS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FOLDER_PATHS, emptySet()) ?: emptySet()
        return set.toList().sorted()
    }

    fun addCustomFolder(context: Context, path: String): Boolean {
        val f = File(path)
        if (!f.exists() || !f.isDirectory) return false
        val prefs = context.getSharedPreferences(PREFS_CUSTOM_FOLDERS, Context.MODE_PRIVATE)
        val set = (prefs.getStringSet(KEY_FOLDER_PATHS, emptySet()) ?: emptySet()).toMutableSet()
        set.add(f.absolutePath)
        prefs.edit().putStringSet(KEY_FOLDER_PATHS, set).apply()
        return true
    }

    fun removeCustomFolder(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_CUSTOM_FOLDERS, Context.MODE_PRIVATE)
        val set = (prefs.getStringSet(KEY_FOLDER_PATHS, emptySet()) ?: emptySet()).toMutableSet()
        set.remove(path)
        prefs.edit().putStringSet(KEY_FOLDER_PATHS, set).apply()
    }

    fun detectVideoOrigin(path: String, bucketName: String? = null): VideoOrigin {
        val lowerPath = path.lowercase(Locale.ROOT)
        val lowerBucket = bucketName?.lowercase(Locale.ROOT) ?: ""
        return when {
            lowerPath.contains("com.whatsapp") || lowerPath.contains("/whatsapp") || lowerBucket.contains("whatsapp") -> VideoOrigin.WHATSAPP
            lowerPath.contains("gmk_downloads") || lowerBucket.contains("gmk_downloads") -> VideoOrigin.GMK_BROWSER
            lowerPath.contains("gmk_transfers") || lowerPath.contains("gmk_received") || lowerPath.contains("received") -> VideoOrigin.RECEIVED
            lowerPath.contains("/dcim") || lowerPath.contains("/camera") || lowerBucket.contains("camera") || lowerBucket.contains("dcim") -> VideoOrigin.CAMERA
            lowerPath.contains("org.telegram") || lowerPath.contains("/telegram") || lowerBucket.contains("telegram") -> VideoOrigin.TELEGRAM
            lowerPath.contains("bluetooth") || lowerBucket.contains("bluetooth") -> VideoOrigin.BLUETOOTH
            lowerPath.contains("/download") || lowerBucket.contains("download") -> VideoOrigin.DOWNLOADS
            else -> VideoOrigin.OTHER
        }
    }

    suspend fun queryVideos(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val itemsMap = LinkedHashMap<String, MediaItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        // 1. Android MediaStore Query (All accessible volumes)
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Video.Media.SIZE} > 0",
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val wCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val name = cursor.getString(nameCol) ?: "Video"
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol)
                    val duration = cursor.getLong(durCol)
                    val w = if (wCol >= 0) cursor.getInt(wCol) else 0
                    val h = if (hCol >= 0) cursor.getInt(hCol) else 0
                    val mod = cursor.getLong(modCol) * 1000
                    val bucketName = if (bucketCol >= 0) cursor.getString(bucketCol) else null

                    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    // Strict separation: never include audio in videos
                    if (AUDIO_EXTENSIONS.contains(ext) || mime?.startsWith("audio/") == true) {
                        continue
                    }
                    if (size <= 0) continue

                    val origin = detectVideoOrigin(path, bucketName)
                    val key = if (path.isNotEmpty()) path else contentUri.toString()

                    itemsMap[key] = MediaItem(
                        uri = contentUri,
                        name = name,
                        path = path,
                        size = size,
                        mimeType = mime,
                        durationMs = duration,
                        resolution = if (w > 0 && h > 0) "${w}x${h}" else null,
                        lastModified = mod,
                        isDirectory = false,
                        mediaType = MediaType.VIDEO,
                        extension = ext,
                        isArchive = false,
                        origin = origin
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Direct directory scan of all permitted standard & app locations to never miss un-indexed videos
        val scanDirs = mutableListOf<File>()

        fun addIfValid(dir: File?) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                scanDirs.add(dir)
            }
        }

        // Standard Android Media folders
        addIfValid(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
        addIfValid(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        addIfValid(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        addIfValid(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))

        val extRoot = Environment.getExternalStorageDirectory()
        if (extRoot != null && extRoot.exists()) {
            // GMK Browser downloads and received transfers
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dlDir != null) {
                addIfValid(File(dlDir, "GMK_Downloads"))
                addIfValid(File(dlDir, "GMK_Transfers"))
                addIfValid(File(dlDir, "GMK_Received"))
            }

            // WhatsApp Video folders
            addIfValid(File(extRoot, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video"))
            addIfValid(File(extRoot, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Sent"))
            addIfValid(File(extRoot, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Private"))
            addIfValid(File(extRoot, "WhatsApp/Media/WhatsApp Video"))
            addIfValid(File(extRoot, "WhatsApp/Media/WhatsApp Video/Sent"))
            addIfValid(File(extRoot, "WhatsApp/Media/WhatsApp Video/Private"))

            // Telegram Video folders
            addIfValid(File(extRoot, "Telegram/Telegram Video"))
            addIfValid(File(extRoot, "Movies/Telegram"))
            addIfValid(File(extRoot, "Download/Telegram"))
            addIfValid(File(extRoot, "Android/data/org.telegram.messenger/files/Telegram/Telegram Video"))

            // Bluetooth & shared media
            addIfValid(File(extRoot, "Bluetooth"))
            addIfValid(File(extRoot, "Download/Bluetooth"))
        }

        // Add custom user-configured folders
        for (customPath in getCustomFolders(context)) {
            addIfValid(File(customPath))
        }

        for (dir in scanDirs) {
            try {
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.isFile && file.length() > 0) {
                        val ext = file.extension.lowercase(Locale.ROOT)
                        if (VIDEO_EXTENSIONS.contains(ext) && !AUDIO_EXTENSIONS.contains(ext)) {
                            val path = file.absolutePath
                            if (!itemsMap.containsKey(path)) {
                                val origin = detectVideoOrigin(path)
                                var duration = 0L
                                var res: String? = null
                                try {
                                    val retriever = MediaMetadataRetriever()
                                    retriever.setDataSource(file.absolutePath)
                                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    duration = durStr?.toLongOrNull() ?: 0L
                                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                                    if (w > 0 && h > 0) res = "${w}x${h}"
                                    retriever.release()
                                } catch (_: Throwable) {}

                                itemsMap[path] = MediaItem(
                                    uri = Uri.fromFile(file),
                                    name = file.name,
                                    path = path,
                                    size = file.length(),
                                    mimeType = FileManagerHelper.getMimeTypeFromExtension(ext) ?: "video/$ext",
                                    durationMs = duration,
                                    resolution = res,
                                    lastModified = file.lastModified(),
                                    isDirectory = false,
                                    mediaType = MediaType.VIDEO,
                                    extension = ext,
                                    isArchive = false,
                                    origin = origin
                                )
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        itemsMap.values.sortedByDescending { it.lastModified }
    }

    suspend fun queryAudios(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )

        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.SIZE} > 0",
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val title = cursor.getString(titleCol) ?: cursor.getString(nameCol) ?: "Audio"
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)
                    val mime = cursor.getString(mimeCol)
                    val duration = cursor.getLong(durCol)
                    val mod = cursor.getLong(modCol) * 1000
                    val ext = path.substringAfterLast('.', "mp3").lowercase(Locale.ROOT)

                    // Strict separation: never include video files in music
                    if (VIDEO_EXTENSIONS.contains(ext) || mime?.startsWith("video/") == true) {
                        continue
                    }

                    list.add(
                        MediaItem(
                            uri = contentUri,
                            name = title,
                            path = path,
                            size = size,
                            mimeType = mime,
                            durationMs = duration,
                            lastModified = mod,
                            isDirectory = false,
                            mediaType = MediaType.AUDIO,
                            extension = ext,
                            isArchive = false
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback: scan Music and Downloads
        if (list.isEmpty()) {
            val scanDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )
            for (dir in scanDirs) {
                if (dir.exists()) {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        val ext = file.extension.lowercase(Locale.ROOT)
                        if (file.isFile && AUDIO_EXTENSIONS.contains(ext) && !VIDEO_EXTENSIONS.contains(ext)) {
                            list.add(FileManagerHelper.fileToMediaItem(file))
                        }
                    }
                }
            }
        }

        list.sortedByDescending { it.lastModified }
    }

    suspend fun queryArchives(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val scanDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(null)
        )

        val seen = mutableSetOf<String>()
        for (dir in scanDirs) {
            if (dir.exists()) {
                try {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        if (file.isFile && FileManagerHelper.isArchiveExtension(file.extension)) {
                            if (seen.add(file.absolutePath)) {
                                list.add(FileManagerHelper.fileToMediaItem(file))
                            }
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        try {
            val root = Environment.getExternalStorageDirectory()
            if (root != null && root.exists()) {
                root.listFiles()?.forEach { file ->
                    if (file.isFile && FileManagerHelper.isArchiveExtension(file.extension)) {
                        if (seen.add(file.absolutePath)) {
                            list.add(FileManagerHelper.fileToMediaItem(file))
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        list.sortedByDescending { it.lastModified }
    }
}
