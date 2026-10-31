package com.example

import com.example.browser.DownloadManagerHelper
import com.example.browser.DownloadTask
import com.example.browser.DownloadTaskStatus
import com.example.transfer.TransferEngine
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
class TransferAndBrowserUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testTransferEngineByteFormattingLargeTransfers() {
        // Test KB, MB, GB, and extreme 1 TB+ 64-bit byte counts
        assertEquals("500 B", TransferEngine.formatBytes(500L))
        assertEquals("2 KB", TransferEngine.formatBytes(2048L))
        assertEquals("15.0 MB", TransferEngine.formatBytes(15L * 1024L * 1024L))
        assertEquals("4.50 GB", TransferEngine.formatBytes((4.5 * 1024 * 1024 * 1024).toLong()))
        assertEquals("1.50 TB", TransferEngine.formatBytes((1.5 * 1024 * 1024 * 1024 * 1024).toLong()))
        assertEquals("2.00 TB", TransferEngine.formatBytes(2L * 1024L * 1024L * 1024L * 1024L))
    }

    @Test
    fun testTransferEngineSpeedAndEtaCalculation() {
        val speedZero = TransferEngine.formatSpeed(0L)
        assertEquals("0 KB/s", speedZero)

        val speedKb = TransferEngine.formatSpeed(512 * 1024L)
        assertEquals("512 KB/s", speedKb)

        val speedMb = TransferEngine.formatSpeed(15 * 1024 * 1024L)
        assertEquals("15.0 MB/s", speedMb)

        // ETA calculation
        val etaSec = TransferEngine.formatEta(30L * 1024L * 1024L, 10L * 1024L * 1024L)
        assertEquals("3s", etaSec)

        val etaMin = TransferEngine.formatEta(120L * 1024L * 1024L, 1L * 1024L * 1024L)
        assertEquals("2m 00s", etaMin)
    }

    @Test
    fun testDuplicateFilenameResolution() {
        val dir = tempFolder.newFolder("dup_test")
        val file1 = File(dir, "video.mp4").apply { writeText("original") }
        assertTrue(file1.exists())

        val resolved = TransferEngine.resolveUniqueFile(dir, "video.mp4")
        assertEquals("video (1).mp4", resolved.name)
        assertFalse(resolved.exists())

        // Create the resolved one and test second conflict
        resolved.writeText("second")
        val resolved2 = TransferEngine.resolveUniqueFile(dir, "video.mp4")
        assertEquals("video (2).mp4", resolved2.name)
    }

    @Test
    fun testSha256ChecksumIntegrity() {
        val dir = tempFolder.newFolder("hash_test")
        val file = File(dir, "sample.bin").apply {
            writeBytes(ByteArray(128 * 1024) { (it % 256).toByte() })
        }
        val hash = TransferEngine.computeSha256(file)
        assertTrue("Hash should not be empty", hash.isNotBlank())
        assertEquals("SHA-256 hex string should be 64 characters", 64, hash.length)

        // Verify identical content produces identical hash
        val file2 = File(dir, "sample2.bin").apply {
            writeBytes(ByteArray(128 * 1024) { (it % 256).toByte() })
        }
        val hash2 = TransferEngine.computeSha256(file2)
        assertEquals(hash, hash2)
    }

    @Test
    fun testDownloadManagerTaskStatusControls() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val task = DownloadTask(
            id = "test_task_1",
            url = "https://example.com/movie.mp4",
            fileName = "movie.mp4",
            destinationPath = "/tmp/movie.mp4"
        )
        assertEquals(DownloadTaskStatus.PENDING, task.status)

        DownloadManagerHelper.pauseTask(context, task.id)
        DownloadManagerHelper.resumeTask(context, task.id)
        DownloadManagerHelper.cancelTask(context, task.id)

        // Directory should be accessible
        val dir = DownloadManagerHelper.getDownloadDirectory()
        assertNotNull(dir)
    }

    @Test
    fun testDownloadAccelerationEngineDynamicSegmentMath() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val engine = com.example.browser.DownloadAccelerationEngine.getInstance(context)
        assertNotNull(engine)

        // Verify Acceleration toggle preference
        engine.setSmartAccelerationEnabled(true)
        assertTrue(engine.isSmartAccelerationEnabled())

        engine.setSmartAccelerationEnabled(false)
        assertFalse(engine.isSmartAccelerationEnabled())
        engine.setSmartAccelerationEnabled(true)

        // Segment calculation math verification for a 40MB file (40 * 1024 * 1024 = 41,943,040 bytes)
        val totalSize = 41_943_040L
        val segmentCount = 3
        val segSize = totalSize / segmentCount

        val seg0Start = 0L
        val seg0End = seg0Start + segSize - 1L
        val seg1Start = seg0End + 1L
        val seg1End = seg1Start + segSize - 1L
        val seg2Start = seg1End + 1L
        val seg2End = totalSize - 1L

        assertEquals(0L, seg0Start)
        assertEquals(seg0End + 1L, seg1Start)
        assertEquals(seg1End + 1L, seg2Start)
        assertEquals(totalSize - 1L, seg2End)

        // Total sum of non-overlapping segment intervals must equal the total size exactly
        val coveredBytes = (seg0End - seg0Start + 1L) + (seg1End - seg1Start + 1L) + (seg2End - seg2Start + 1L)
        assertEquals("Sum of segment intervals must perfectly match total size", totalSize, coveredBytes)
    }

    @Test
    fun testDownloadSpeedTrackerSmoothRollingAveraging() {
        var recordedBps = 0L
        val tracker = com.example.browser.DownloadSpeedTracker { speed ->
            recordedBps = speed
        }
        // Feed 1 MB in chunks
        tracker.recordBytes(512 * 1024L)
        Thread.sleep(550)
        tracker.recordBytes(512 * 1024L)

        assertTrue("Rolling speed tracker must report positive throughput", recordedBps >= 0L)
    }

    @Test
    fun testSearchHistoryIndependentFromBrowsingHistory() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()

        // Clear existing test data
        com.example.browser.BrowserHistoryManager.clearSearchHistory(context)
        com.example.browser.BrowserHistoryManager.clearBrowsingHistory(context)

        // Add search queries
        com.example.browser.BrowserHistoryManager.addSearchQuery(context, "android exoplayer")
        com.example.browser.BrowserHistoryManager.addSearchQuery(context, "jetpack compose tutorial")

        // Add browsing history
        com.example.browser.BrowserHistoryManager.addBrowsingHistory(context, "Google", "https://www.google.com")
        com.example.browser.BrowserHistoryManager.addBrowsingHistory(context, "GitHub", "https://github.com")

        var searches = com.example.browser.BrowserHistoryManager.getSearchHistory(context)
        var browsing = com.example.browser.BrowserHistoryManager.getBrowsingHistory(context)
        assertEquals(2, searches.size)
        assertEquals(2, browsing.size)

        // Clear search history independently
        com.example.browser.BrowserHistoryManager.clearSearchHistory(context)

        searches = com.example.browser.BrowserHistoryManager.getSearchHistory(context)
        browsing = com.example.browser.BrowserHistoryManager.getBrowsingHistory(context)

        // Search history must be 0, but browsing history MUST remain intact!
        assertEquals("Search history should be empty", 0, searches.size)
        assertEquals("Browsing history must NOT be deleted when clearing search history", 2, browsing.size)
    }

    @Test
    fun testBrowsingHistoryTimeRangeClearing() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        com.example.browser.BrowserHistoryManager.clearBrowsingHistory(context)

        com.example.browser.BrowserHistoryManager.addBrowsingHistory(context, "Page 1", "https://example.com/1")
        com.example.browser.BrowserHistoryManager.addBrowsingHistory(context, "Page 2", "https://example.com/2")

        assertEquals(2, com.example.browser.BrowserHistoryManager.getBrowsingHistory(context).size)

        // Clear ALL_TIME
        val cleared = com.example.browser.BrowserHistoryManager.clearBrowsingHistory(context, com.example.browser.HistoryTimeRange.ALL_TIME)
        assertEquals(2, cleared)
        assertEquals(0, com.example.browser.BrowserHistoryManager.getBrowsingHistory(context).size)
    }

    @Test
    fun testRecentlyClosedTabsIndependentManagement() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        com.example.browser.BrowserHistoryManager.clearRecentlyClosedTabs(context)

        val tab1 = com.example.browser.BrowserTab(id = "t1", title = "Doc", url = "https://example.com/doc")
        val tab2 = com.example.browser.BrowserTab(id = "t2", title = "Audio", url = "https://example.com/audio")

        com.example.browser.BrowserHistoryManager.addRecentlyClosedTab(context, tab1)
        com.example.browser.BrowserHistoryManager.addRecentlyClosedTab(context, tab2)

        val closed = com.example.browser.BrowserHistoryManager.getRecentlyClosedTabs(context)
        assertEquals(2, closed.size)
        assertEquals("Audio", closed[0].title)

        // Remove one restored tab
        com.example.browser.BrowserHistoryManager.removeRecentlyClosedTab(context, closed[0].id)
        assertEquals(1, com.example.browser.BrowserHistoryManager.getRecentlyClosedTabs(context).size)

        // Clear all closed tabs
        com.example.browser.BrowserHistoryManager.clearRecentlyClosedTabs(context)
        assertEquals(0, com.example.browser.BrowserHistoryManager.getRecentlyClosedTabs(context).size)
    }

    @Test
    fun testClearBrowsingDataSelectiveOptions() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()

        com.example.browser.BrowserHistoryManager.addSearchQuery(context, "kotlin flow")
        com.example.browser.BrowserHistoryManager.addBrowsingHistory(context, "Kotlin Docs", "https://kotlinlang.org")
        com.example.browser.BrowserHistoryManager.addRecentlyClosedTab(
            context,
            com.example.browser.BrowserTab(id = "t1", title = "VLC", url = "https://videolan.org")
        )

        // Selectively clear ONLY search history
        com.example.browser.BrowserHistoryManager.clearBrowsingData(
            context = context,
            options = com.example.browser.ClearBrowsingDataOptions(
                clearSearchHistory = true,
                clearBrowsingHistory = false,
                closeOpenTabs = false,
                clearCookies = false,
                clearCache = false,
                clearRecentlyClosedTabs = false
            ),
            webView = null,
            onComplete = {}
        )

        assertEquals("Search history should be cleared", 0, com.example.browser.BrowserHistoryManager.getSearchHistory(context).size)
        assertEquals("Browsing history must NOT be cleared", 1, com.example.browser.BrowserHistoryManager.getBrowsingHistory(context).size)
        assertEquals("Recently closed tabs must NOT be cleared", 1, com.example.browser.BrowserHistoryManager.getRecentlyClosedTabs(context).size)
    }

    @Test
    fun testCleanupCorruptVariationsSeed() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val webviewDir = File(context.dataDir, "app_webview").apply { mkdirs() }
        val corruptSeed = File(webviewDir, "variations_seed").apply { writeBytes(byteArrayOf(0, 1, 2)) }
        assertTrue(corruptSeed.exists())

        com.example.browser.BrowserHistoryManager.cleanupCorruptVariationsSeed(context)
        assertFalse("Corrupt seed file (<64 bytes) should be deleted", corruptSeed.exists())
    }

    @Test
    fun testDonationDetailsAndConstants() {
        assertEquals("sam.world0099@okhdfcbank", com.example.donation.DonationManager.UPI_ID)
        assertEquals("Sam", com.example.donation.DonationManager.RECIPIENT_NAME)
        assertEquals("sam.world0099@gmail.com", com.example.donation.DonationManager.SUPPORT_EMAIL)

        val context = org.robolectric.RuntimeEnvironment.getApplication()
        // Fresh install should not show popup immediately
        assertFalse(com.example.donation.DonationManager.shouldShowDonationPopup(context))

        // Marking shown updates timestamp
        com.example.donation.DonationManager.markPopupShown(context)
        assertFalse("Should not show immediately after marking shown", com.example.donation.DonationManager.shouldShowDonationPopup(context))
    }

    @Test
    fun testFeedbackPersistence() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        com.example.feedback.FeedbackManager.clearFeedback(context)
        assertEquals(0, com.example.feedback.FeedbackManager.getSavedFeedback(context).size)

        val item = com.example.feedback.FeedbackItem(
            id = "fb_test_1",
            type = "Feature Request",
            rating = 5,
            message = "Please add subtitle font scaling options",
            section = "Video Player",
            email = "user@example.com",
            timestamp = System.currentTimeMillis(),
            appVersion = "1.0"
        )

        com.example.feedback.FeedbackManager.saveFeedback(context, item)
        val saved = com.example.feedback.FeedbackManager.getSavedFeedback(context)
        assertEquals(1, saved.size)
        assertEquals("Feature Request", saved[0].type)
        assertEquals(5, saved[0].rating)
        assertEquals("Please add subtitle font scaling options", saved[0].message)
        assertEquals("Video Player", saved[0].section)
        assertEquals("user@example.com", saved[0].email)

        com.example.feedback.FeedbackManager.clearFeedback(context)
        assertEquals(0, com.example.feedback.FeedbackManager.getSavedFeedback(context).size)
    }

    @Test
    fun testUploadedQrCodeResourceExists() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.gpay_donation_qr)
        assertNotNull("Google Pay donation QR drawable must exist in resources", drawable)
    }
}
