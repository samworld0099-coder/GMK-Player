package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.trash.TrashManager
import com.example.vault.VaultManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TrashAndVaultRobolectricTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testTrashManagerLifecycle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workDir = tempFolder.newFolder("trash_test")
        val testFile = File(workDir, "secret_plan.pdf").apply { writeText("Confidential content") }

        // Move to trash
        val moveResult = TrashManager.moveToTrash(context, testFile, isPrivate = false)
        assertTrue("Move to trash should succeed", moveResult.isSuccess)
        assertFalse("Original file should no longer exist", testFile.exists())

        // Get trash items
        val trashItems = TrashManager.listTrash(context, isPrivate = false)
        val trashedItem = trashItems.find { it.fileName == "secret_plan.pdf" }
        assertNotNull("Item must be found in trash", trashedItem)

        // Restore file
        val restoreResult = TrashManager.restoreItem(context, trashedItem!!, isPrivate = false)
        assertTrue("Restore should succeed", restoreResult.isSuccess)
        assertTrue("Original file should exist again", testFile.exists())
        assertEquals("Confidential content", testFile.readText())

        // Move to trash again and permanently delete
        val moveAgain = TrashManager.moveToTrash(context, testFile, isPrivate = false)
        assertTrue(moveAgain.isSuccess)
        val trashItems2 = TrashManager.listTrash(context, isPrivate = false)
        val trashedItem2 = trashItems2.find { it.fileName == "secret_plan.pdf" }
        assertNotNull(trashedItem2)

        val deleteResult = TrashManager.permanentlyDelete(context, trashedItem2!!, isPrivate = false)
        assertTrue("Permanent delete should succeed", deleteResult.isSuccess)
        assertFalse("File must be gone from storage", trashedItem2.trashedFile.exists())
    }

    @Test
    fun testVaultManagerEncryptionAndDecryption() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workDir = tempFolder.newFolder("vault_test")
        val pin = "1234"
        val wrongPin = "9999"

        // Setup PIN
        val setupResult = VaultManager.setupVaultPin(context, pin)
        assertTrue("PIN setup must succeed", setupResult)
        assertTrue("Vault must have PIN configured", VaultManager.isVaultSetup(context))
        assertTrue("Correct PIN verification must pass", VaultManager.verifyPin(context, pin))
        assertFalse("Wrong PIN verification must fail", VaultManager.verifyPin(context, wrongPin))

        // Encrypt file into vault
        val originalFile = File(workDir, "my_diary.txt").apply { writeText("My private diary entry with AES-GCM") }
        val importResult = VaultManager.importFile(
            context = context,
            sourceFile = originalFile,
            pin = pin,
            album = "Personal",
            deleteOriginal = true
        )
        assertTrue("Import and encryption must succeed", importResult.isSuccess)
        assertFalse("Original unencrypted file must be removed", originalFile.exists())

        val vaultFiles = VaultManager.listVaultFiles(context)
        val vaulted = vaultFiles.find { it.name == "my_diary.txt" }
        assertNotNull("Vaulted file should be listed", vaulted)
        assertEquals("Personal", vaulted!!.albumName)

        // Verify in-cache decryption
        val decryptedResult = VaultManager.decryptFileToCache(context, vaulted, pin)
        assertTrue("Decryption to cache must succeed", decryptedResult.isSuccess)
        val plainFile = decryptedResult.getOrNull()!!
        assertTrue("Decrypted temp file must exist", plainFile.exists())
        assertEquals("My private diary entry with AES-GCM", plainFile.readText())

        // Export/decrypt to public folder
        val exportDir = File(workDir, "exported").apply { mkdirs() }
        val exportResult = VaultManager.exportFile(context, vaulted, pin, exportDir)
        assertTrue("Export from vault must succeed", exportResult.isSuccess)
        val exportedFile = exportResult.getOrNull()!!
        assertTrue("Exported file must exist", exportedFile.exists())
        assertEquals("My private diary entry with AES-GCM", exportedFile.readText())
    }
}
