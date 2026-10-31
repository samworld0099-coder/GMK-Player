package com.example

import com.example.archive.ArchiveManager
import com.example.archive.ArchiveTreeHelper
import com.example.filemanager.ConflictResolution
import com.example.filemanager.FileManagerHelper
import com.example.filemanager.MediaStoreHelper
import com.example.player.video.ResizeMode
import com.example.player.video.TrackInfo
import com.example.player.video.VideoPlayerUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveAndPlayerUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCreateAndReadStandardZipArchive() = runBlocking {
        val workDir = tempFolder.newFolder("zip_test")
        val file1 = File(workDir, "test1.txt").apply { writeText("Hello GMK Media Player") }
        val subDir = File(workDir, "subfolder").apply { mkdirs() }
        val file2 = File(subDir, "video_notes.md").apply { writeText("Video playback notes") }

        val outZip = File(workDir, "test.zip")
        val createResult = ArchiveManager.createZipArchive(
            sources = listOf(file1, subDir),
            outputZipFile = outZip,
            onProgress = { _, _ -> }
        )
        assertTrue("Zip creation must succeed", createResult.isSuccess)
        assertTrue("Zip file must exist", outZip.exists())

        val listResult = ArchiveManager.listEntries(outZip)
        assertTrue("Zip listing must succeed", listResult.isSuccess)
        val entries = listResult.getOrNull()
        assertNotNull(entries)
        assertTrue("Should contain entries", entries!!.size >= 2)
        assertTrue("Should contain test1.txt", entries.any { it.name == "test1.txt" })
    }

    @Test
    fun testNestedArchiveVirtualTreeNavigation() = runBlocking {
        val workDir = tempFolder.newFolder("tree_test")
        val dirA = File(workDir, "movies/action").apply { mkdirs() }
        val movieFile = File(dirA, "clip.mp4").apply { writeText("dummy video content") }

        val outZip = File(workDir, "movies.zip")
        ArchiveManager.createZipArchive(listOf(dirA), outZip, onProgress = { _, _ -> })

        val entries = ArchiveManager.listEntries(outZip).getOrNull()!!
        val rootNode = ArchiveTreeHelper.getEntriesInDirectory(entries, "")
        assertTrue("Root should have movies folder", rootNode.subfolders.any { it.name == "movies" || it.name == "action" })

        val crumbs = ArchiveTreeHelper.getBreadcrumbs("movies/action")
        assertEquals(3, crumbs.size) // Root, movies, action
    }

    @Test
    fun testUnicodeAndSpecialFilenamesInZip() = runBlocking {
        val workDir = tempFolder.newFolder("unicode_test")
        val unicodeFile = File(workDir, "日本語_subtitle_🔥.srt").apply { writeText("1\n00:00:01,000 --> 00:00:04,000\nテスト字幕") }

        val outZip = File(workDir, "unicode.zip")
        ArchiveManager.createZipArchive(listOf(unicodeFile), outZip, onProgress = { _, _ -> })

        val entries = ArchiveManager.listEntries(outZip).getOrNull()!!
        assertTrue("Unicode entry should be listed", entries.any { it.name.contains("日本語") })
    }

    @Test
    fun testPathTraversalSecurityProtection() = runBlocking {
        val workDir = tempFolder.newFolder("security_test")
        val maliciousZip = File(workDir, "malicious.zip")

        // Manually build zip containing path traversal entry "../outside.txt"
        ZipOutputStream(FileOutputStream(maliciousZip)).use { zos ->
            val entry = ZipEntry("../outside.txt")
            zos.putNextEntry(entry)
            zos.write("Hacked file content".toByteArray())
            zos.closeEntry()
        }

        val extractDest = File(workDir, "extracted_safe")
        extractDest.mkdirs()

        val extractResult = ArchiveManager.extract(
            archiveFile = maliciousZip,
            destinationDir = extractDest,
            onProgress = {},
            isCancelled = { false }
        )

        // Must reject path traversal attack with failure
        assertTrue("Should detect and fail malicious path traversal entry", extractResult.isFailure)
        assertFalse("Should not create outside.txt outside dest", File(workDir, "outside.txt").exists())
    }

    @Test
    fun testSelectiveExtractionAndExtractionComplete() = runBlocking {
        val workDir = tempFolder.newFolder("extract_test")
        val fileA = File(workDir, "keep.txt").apply { writeText("Extract this file") }
        val fileB = File(workDir, "skip.txt").apply { writeText("Do not extract this") }

        val zipFile = File(workDir, "bundle.zip")
        ArchiveManager.createZipArchive(listOf(fileA, fileB), zipFile, onProgress = { _, _ -> })

        val outDir = File(workDir, "output")
        val result = ArchiveManager.extract(
            archiveFile = zipFile,
            destinationDir = outDir,
            selectedPaths = setOf("keep.txt"),
            onProgress = {},
            isCancelled = { false }
        )

        assertTrue(result.isSuccess)
        assertTrue(File(outDir, "keep.txt").exists())
        assertFalse(File(outDir, "skip.txt").exists())
    }

    @Test
    fun testCorruptedArchiveHandling() = runBlocking {
        val workDir = tempFolder.newFolder("corrupt_test")
        val corruptZip = File(workDir, "corrupted.zip").apply {
            writeText("NOT A REAL ZIP FILE HEADER RANDOM BYTES 1234567890")
        }

        val listResult = ArchiveManager.listEntries(corruptZip)
        assertTrue("Should report failure on corrupted archive", listResult.isFailure)
    }

    @Test
    fun testCancelledExtractionTerminates() = runBlocking {
        val workDir = tempFolder.newFolder("cancel_test")
        val fileA = File(workDir, "large.txt").apply { writeText("A".repeat(50000)) }
        val zipFile = File(workDir, "cancel.zip")
        ArchiveManager.createZipArchive(listOf(fileA), zipFile, onProgress = { _, _ -> })

        val destDir = File(workDir, "cancel_out")
        val result = ArchiveManager.extract(
            archiveFile = zipFile,
            destinationDir = destDir,
            onProgress = {},
            isCancelled = { true } // Cancelled immediately
        )
        assertTrue("Cancelled extraction should return failure", result.isFailure)
    }

    @Test
    fun testDuplicateFilenameRenameConflict() = runBlocking {
        val workDir = tempFolder.newFolder("dup_test")
        val srcDir = File(workDir, "src").apply { mkdirs() }
        val destDir = File(workDir, "dest").apply { mkdirs() }

        val file1 = File(srcDir, "movie.mp4").apply { writeText("Video 1") }
        File(destDir, "movie.mp4").apply { writeText("Existing Video") }

        val result = FileManagerHelper.copy(file1, destDir, ConflictResolution.RENAME)
        assertTrue("Copy should succeed", result.isSuccess)
        val copied = result.getOrNull()!!
        assertEquals("movie (1).mp4", copied.name)
        assertTrue("Destination should have both files", File(destDir, "movie.mp4").exists() && File(destDir, "movie (1).mp4").exists())
    }

    @Test
    fun testVideoPlayerStateManagement() {
        var state = VideoPlayerUiState(
            title = "MyMovie.mp4",
            durationMs = 120000L,
            currentPositionMs = 30000L,
            playbackSpeed = 1.0f,
            resizeMode = ResizeMode.FIT
        )

        // Test speed switching
        state = state.copy(playbackSpeed = 1.5f)
        assertEquals(1.5f, state.playbackSpeed)

        // Test resize mode switching
        state = state.copy(resizeMode = ResizeMode.ZOOM)
        assertEquals(ResizeMode.ZOOM, state.resizeMode)

        // Test rotation 90 degrees
        state = state.copy(rotationDegrees = (state.rotationDegrees + 90) % 360)
        assertEquals(90, state.rotationDegrees)

        // Test audio track selection representation
        val track = TrackInfo(
            id = "audio_1",
            index = 0,
            groupIndex = 1,
            label = "English (5.1 surround)",
            language = "en",
            isSelected = true
        )
        state = state.copy(audioTracks = listOf(track))
        assertEquals("English (5.1 surround)", state.audioTracks.first().label)
    }

    @Test
    fun testDoubleTapSeekMathAndBoundsClamping() {
        val duration = 60_000L // 60 seconds
        val initialPos = 5_000L // 5 seconds

        // Backward double-tap seek (-10s) must clamp at 0L
        val backwardTarget = (initialPos - 10_000L).coerceAtLeast(0L)
        assertEquals(0L, backwardTarget)

        // Forward double-tap seek (+10s)
        val forwardTarget = (initialPos + 10_000L).coerceIn(0L, duration)
        assertEquals(15_000L, forwardTarget)

        // Forward near the end must clamp at duration
        val nearEndPos = 55_000L
        val clampedEndTarget = (nearEndPos + 10_000L).coerceIn(0L, duration)
        assertEquals(60_000L, clampedEndTarget)
    }

    @Test
    fun testHorizontalSwipeSeekCalculations() {
        val durationMs = 120_000L // 2 minutes
        val startPos = 30_000L // 30 seconds
        val screenWidth = 1080f

        // Swipe right by 540px (half screen)
        val maxSwipeWindowMs = minOf(durationMs, maxOf(60_000L, durationMs / 4))
        val fractionRight = (540f / screenWidth).coerceIn(-1f, 1f) // 0.5
        val deltaMsRight = (fractionRight * maxSwipeWindowMs).toLong()
        val targetMsRight = (startPos + deltaMsRight).coerceIn(0L, durationMs)

        assertTrue("Swiping right must increase playback position", targetMsRight > startPos)
        assertTrue("Target position must remain within video duration", targetMsRight <= durationMs)

        // Swipe left by 800px
        val fractionLeft = (-800f / screenWidth).coerceIn(-1f, 1f)
        val deltaMsLeft = (fractionLeft * maxSwipeWindowMs).toLong()
        val targetMsLeft = (startPos + deltaMsLeft).coerceIn(0L, durationMs)

        assertTrue("Swiping left must decrease playback position", targetMsLeft < startPos)
        assertTrue("Target position must never be negative", targetMsLeft >= 0L)
    }

    @Test
    fun test4KAnd2KResolutionClassification() {
        fun classify(w: Int, h: Int): String {
            return when {
                w >= 3840 && h >= 2160 -> "4K"
                w >= 2560 && h >= 1440 -> "2K"
                w >= 1920 && h >= 1080 -> "1080p"
                else -> "SD"
            }
        }

        assertEquals("4K", classify(3840, 2160))
        assertEquals("2K", classify(2560, 1440))
        assertEquals("1080p", classify(1920, 1080))
    }

    @Test
    fun testSubtitleDelaySyncAdjustment() {
        var state = VideoPlayerUiState(subtitleDelayMs = 0L)
        assertEquals(0L, state.subtitleDelayMs)

        // Add 250ms delay
        state = state.copy(subtitleDelayMs = state.subtitleDelayMs + 250L)
        assertEquals(250L, state.subtitleDelayMs)

        // Advance by -500ms (subtitle appears earlier)
        state = state.copy(subtitleDelayMs = state.subtitleDelayMs - 500L)
        assertEquals(-250L, state.subtitleDelayMs)

        // Reset to 0
        state = state.copy(subtitleDelayMs = 0L)
        assertEquals(0L, state.subtitleDelayMs)
    }

    @Test
    fun testVideoOriginDetectionAcrossDirectories() {
        assertEquals(
            com.example.data.model.VideoOrigin.WHATSAPP,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/VID_2026.mp4")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.GMK_BROWSER,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/Download/GMK_Downloads/online_lecture.mp4")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.CAMERA,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/DCIM/Camera/VID_20261030_120000.mp4")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.TELEGRAM,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/Telegram/Telegram Video/clip.mp4")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.BLUETOOTH,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/Bluetooth/transfer.mp4")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.RECEIVED,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/Download/GMK_Transfers/share_me.mkv")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.DOWNLOADS,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/Download/documentary.mp4")
        )
        assertEquals(
            com.example.data.model.VideoOrigin.OTHER,
            MediaStoreHelper.detectVideoOrigin("/storage/emulated/0/CustomDir/movie.mp4")
        )
    }

    @Test
    fun testStrictSeparationOfVideoAndAudioExtensions() {
        val videoExts = MediaStoreHelper.VIDEO_EXTENSIONS
        val audioExts = MediaStoreHelper.AUDIO_EXTENSIONS

        // Ensure disjoint sets (no overlap)
        val intersection = videoExts.intersect(audioExts)
        assertTrue("Video and Audio extension sets must be strictly mutually exclusive", intersection.isEmpty())

        // Common formats check
        assertTrue("MP4 must be video", videoExts.contains("mp4"))
        assertTrue("MKV must be video", videoExts.contains("mkv"))
        assertTrue("WebM must be video", videoExts.contains("webm"))

        assertTrue("MP3 must be audio", audioExts.contains("mp3"))
        assertTrue("FLAC must be audio", audioExts.contains("flac"))
        assertTrue("M4A must be audio", audioExts.contains("m4a"))
        assertFalse("MP3 must never be video", videoExts.contains("mp3"))
        assertFalse("MP4 must never be audio", audioExts.contains("mp4"))
    }
}
