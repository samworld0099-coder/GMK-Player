package com.example.trash

import android.content.Context
import com.example.filemanager.FileManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class TrashItem(
    val id: String,
    val originalPath: String,
    val fileName: String,
    val size: Long,
    val deletedTimestamp: Long,
    val trashedFile: File,
    val isDirectory: Boolean
) {
    val formattedSize: String
        get() {
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
}

object TrashManager {

    private fun getTrashDir(context: Context, isPrivate: Boolean): File {
        val base = context.filesDir
        val name = if (isPrivate) ".gmk_vault_trash" else ".gmk_normal_trash"
        val dir = File(base, name)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(context: Context, isPrivate: Boolean): File {
        return File(getTrashDir(context, isPrivate), "trash_metadata.json")
    }

    suspend fun moveToTrash(context: Context, file: File, isPrivate: Boolean = false): Result<TrashItem> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext Result.failure(IllegalArgumentException("File does not exist: ${file.path}"))

        val trashDir = getTrashDir(context, isPrivate)
        val id = "trash_${System.currentTimeMillis()}_${file.name.hashCode()}"
        val targetFile = File(trashDir, "${id}_${file.name}")

        try {
            val isDir = file.isDirectory
            val size = if (isDir) {
                file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            } else {
                file.length()
            }

            // Move to trash directory
            if (isDir) {
                file.copyRecursively(targetFile, overwrite = true)
                file.deleteRecursively()
            } else {
                file.copyTo(targetFile, overwrite = true)
                file.delete()
            }

            val item = TrashItem(
                id = id,
                originalPath = file.absolutePath,
                fileName = file.name,
                size = size,
                deletedTimestamp = System.currentTimeMillis(),
                trashedFile = targetFile,
                isDirectory = isDir
            )

            saveMetadataItem(context, item, isPrivate)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listTrash(context: Context, isPrivate: Boolean = false): List<TrashItem> = withContext(Dispatchers.IO) {
        val metaFile = getMetaFile(context, isPrivate)
        if (!metaFile.exists()) return@withContext emptyList()

        try {
            val jsonStr = metaFile.readText()
            val root = JSONObject(jsonStr)
            val items = mutableListOf<TrashItem>()
            val trashDir = getTrashDir(context, isPrivate)

            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val trashedPath = obj.getString("trashedFile")
                val tFile = File(trashedPath)
                if (tFile.exists()) {
                    items.add(
                        TrashItem(
                            id = key,
                            originalPath = obj.getString("originalPath"),
                            fileName = obj.getString("fileName"),
                            size = obj.getLong("size"),
                            deletedTimestamp = obj.getLong("deletedTimestamp"),
                            trashedFile = tFile,
                            isDirectory = obj.optBoolean("isDirectory", false)
                        )
                    )
                }
            }
            items.sortedByDescending { it.deletedTimestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun restoreItem(context: Context, item: TrashItem, isPrivate: Boolean = false): Result<File> = withContext(Dispatchers.IO) {
        try {
            var dest = File(item.originalPath)
            dest.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            if (dest.exists()) {
                // If destination conflict, restore with copy tag
                val base = dest.nameWithoutExtension
                val ext = if (dest.extension.isNotEmpty()) ".${dest.extension}" else ""
                dest = File(dest.parentFile, "$base (Restored)$ext")
            }

            if (item.isDirectory) {
                item.trashedFile.copyRecursively(dest, overwrite = true)
                item.trashedFile.deleteRecursively()
            } else {
                item.trashedFile.copyTo(dest, overwrite = true)
                item.trashedFile.delete()
            }

            removeMetadataItem(context, item.id, isPrivate)
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun permanentlyDelete(context: Context, item: TrashItem, isPrivate: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (item.isDirectory) item.trashedFile.deleteRecursively()
            else item.trashedFile.delete()
            removeMetadataItem(context, item.id, isPrivate)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun emptyTrash(context: Context, isPrivate: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val trashDir = getTrashDir(context, isPrivate)
            trashDir.listFiles()?.forEach { file ->
                if (file.isDirectory) file.deleteRecursively()
                else file.delete()
            }
            val metaFile = getMetaFile(context, isPrivate)
            if (metaFile.exists()) metaFile.delete()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveMetadataItem(context: Context, item: TrashItem, isPrivate: Boolean) {
        val metaFile = getMetaFile(context, isPrivate)
        val root = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
        } else JSONObject()

        val obj = JSONObject().apply {
            put("originalPath", item.originalPath)
            put("fileName", item.fileName)
            put("size", item.size)
            put("deletedTimestamp", item.deletedTimestamp)
            put("trashedFile", item.trashedFile.absolutePath)
            put("isDirectory", item.isDirectory)
        }
        root.put(item.id, obj)
        metaFile.writeText(root.toString())
    }

    private fun removeMetadataItem(context: Context, id: String, isPrivate: Boolean) {
        val metaFile = getMetaFile(context, isPrivate)
        if (!metaFile.exists()) return
        try {
            val root = JSONObject(metaFile.readText())
            root.remove(id)
            metaFile.writeText(root.toString())
        } catch (_: Exception) {}
    }
}
