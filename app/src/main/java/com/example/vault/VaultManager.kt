package com.example.vault

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.filemanager.FileManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class VaultFileItem(
    val id: String,
    val name: String,
    val encryptedFile: File,
    val albumName: String,
    val mediaType: MediaType,
    val size: Long,
    val importedTimestamp: Long
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

object VaultManager {

    private const val PREFS_NAME = "gmk_vault_prefs"
    private const val KEY_PIN_HASH = "vault_pin_hash"
    private const val KEY_SALT = "vault_salt"
    private const val KEY_IS_SETUP = "vault_is_setup"
    private const val ALGO_AES = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getVaultRootDir(context: Context): File {
        val dir = File(context.filesDir, ".gmk_vault")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(context: Context): File {
        return File(getVaultRootDir(context), "vault_registry.json")
    }

    fun isVaultSetup(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_SETUP, false)
    }

    fun setupVaultPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hash = hashPin(pin, saltHex)

        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_SALT, saltHex)
            .putBoolean(KEY_IS_SETUP, true)
            .apply()
        return true
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = getPrefs(context)
        val expectedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val inputHash = hashPin(pin, salt)
        return expectedHash == inputHash
    }

    fun changePin(context: Context, currentPin: String, newPin: String): Boolean {
        if (!verifyPin(context, currentPin) || newPin.length < 4) return false
        return setupVaultPin(context, newPin)
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$pin:$saltHex"
        val bytes = md.digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(pin: String, saltHex: String): SecretKey {
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest("GMK_KEY:$pin:$saltHex".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGO_AES)
    }

    suspend fun importFile(
        context: Context,
        sourceFile: File,
        pin: String,
        album: String = "Main",
        deleteOriginal: Boolean = true
    ): Result<VaultFileItem> = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext Result.failure(IllegalArgumentException("Source file not found"))
        val salt = getPrefs(context).getString(KEY_SALT, "") ?: ""
        val secretKey = deriveKey(pin, salt)

        val id = "v_${System.currentTimeMillis()}_${sourceFile.name.hashCode()}"
        val vaultDir = getVaultRootDir(context)
        val encryptedFile = File(vaultDir, "$id.enc")

        try {
            // Generate random IV
            val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            FileOutputStream(encryptedFile).use { fos ->
                fos.write(iv) // Prepend IV
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(sourceFile).use { fis ->
                        fis.copyTo(cos)
                    }
                }
            }

            val ext = sourceFile.extension
            val mediaType = FileManagerHelper.determineMediaType(ext, false)
            val item = VaultFileItem(
                id = id,
                name = sourceFile.name,
                encryptedFile = encryptedFile,
                albumName = album,
                mediaType = mediaType,
                size = sourceFile.length(),
                importedTimestamp = System.currentTimeMillis()
            )

            recordVaultItem(context, item)

            if (deleteOriginal) {
                sourceFile.delete()
            }

            Result.success(item)
        } catch (e: Exception) {
            encryptedFile.delete()
            Result.failure(e)
        }
    }

    suspend fun decryptFileToCache(
        context: Context,
        item: VaultFileItem,
        pin: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!item.encryptedFile.exists()) return@withContext Result.failure(IllegalArgumentException("Encrypted vault file missing"))
        val salt = getPrefs(context).getString(KEY_SALT, "") ?: ""
        val secretKey = deriveKey(pin, salt)

        val cacheDir = File(context.cacheDir, "vault_preview")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val tempPlainFile = File(cacheDir, item.name)

        try {
            FileInputStream(item.encryptedFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val readIv = fis.read(iv)
                if (readIv != GCM_IV_LENGTH) return@withContext Result.failure(IllegalStateException("Corrupted vault entry"))

                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(tempPlainFile).use { fos ->
                        cis.copyTo(fos)
                    }
                }
            }
            Result.success(tempPlainFile)
        } catch (e: Exception) {
            tempPlainFile.delete()
            Result.failure(e)
        }
    }

    suspend fun exportFile(
        context: Context,
        item: VaultFileItem,
        pin: String,
        destinationDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val decryptRes = decryptFileToCache(context, item, pin)
        if (decryptRes.isFailure) return@withContext Result.failure(decryptRes.exceptionOrNull()!!)

        val temp = decryptRes.getOrNull()!!
        var dest = File(destinationDir, item.name)
        if (dest.exists()) {
            val base = item.name.substringBeforeLast('.')
            val ext = if (item.name.contains('.')) ".${item.name.substringAfterLast('.')}" else ""
            dest = File(destinationDir, "$base (Exported)$ext")
        }

        try {
            temp.copyTo(dest, overwrite = true)
            temp.delete()
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listVaultFiles(context: Context, albumFilter: String? = null): List<VaultFileItem> = withContext(Dispatchers.IO) {
        val metaFile = getMetaFile(context)
        if (!metaFile.exists()) return@withContext emptyList()
        try {
            val root = JSONObject(metaFile.readText())
            val list = mutableListOf<VaultFileItem>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.getJSONObject(key)
                val album = obj.optString("albumName", "Main")
                if (albumFilter == null || album.equals(albumFilter, ignoreCase = true)) {
                    val encPath = obj.getString("encryptedFile")
                    val encFile = File(encPath)
                    if (encFile.exists()) {
                        list.add(
                            VaultFileItem(
                                id = key,
                                name = obj.getString("name"),
                                encryptedFile = encFile,
                                albumName = album,
                                mediaType = MediaType.valueOf(obj.optString("mediaType", "OTHER")),
                                size = obj.getLong("size"),
                                importedTimestamp = obj.getLong("importedTimestamp")
                            )
                        )
                    }
                }
            }
            list.sortedByDescending { it.importedTimestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAlbums(context: Context): List<String> = withContext(Dispatchers.IO) {
        val all = listVaultFiles(context)
        val albums = all.map { it.albumName }.distinct().toMutableList()
        if (!albums.contains("Main")) albums.add(0, "Main")
        albums
    }

    suspend fun deleteVaultItem(context: Context, item: VaultFileItem): Boolean = withContext(Dispatchers.IO) {
        try {
            item.encryptedFile.delete()
            val metaFile = getMetaFile(context)
            if (metaFile.exists()) {
                val root = JSONObject(metaFile.readText())
                root.remove(item.id)
                metaFile.writeText(root.toString())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun recordVaultItem(context: Context, item: VaultFileItem) {
        val metaFile = getMetaFile(context)
        val root = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
        } else JSONObject()

        val obj = JSONObject().apply {
            put("name", item.name)
            put("encryptedFile", item.encryptedFile.absolutePath)
            put("albumName", item.albumName)
            put("mediaType", item.mediaType.name)
            put("size", item.size)
            put("importedTimestamp", item.importedTimestamp)
        }
        root.put(item.id, obj)
        metaFile.writeText(root.toString())
    }
}
