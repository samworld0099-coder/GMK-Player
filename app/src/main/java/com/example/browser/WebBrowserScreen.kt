package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.MediaItem
import com.example.filemanager.FileManagerHelper
import com.example.transfer.TransferEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class BrowserTab(
    val id: String,
    var title: String = "Web",
    var url: String = "https://www.google.com"
)

data class BookmarkItem(
    val title: String,
    val url: String
)

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBrowserScreen(
    onOpenDownloadedMedia: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val prefs = remember { context.getSharedPreferences("gmk_web_prefs", Context.MODE_PRIVATE) }

    // Tabs
    val tabs = remember { mutableStateListOf(BrowserTab(id = "tab_1", title = "Google", url = "https://www.google.com")) }
    var currentTabIndex by remember { mutableIntStateOf(0) }
    var isIncognito by remember { mutableStateOf(false) }

    // Navigation State
    var urlInput by remember { mutableStateOf("https://www.google.com") }
    var pageTitle by remember { mutableStateOf("Web") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var isHttps by remember { mutableStateOf(true) }

    // Settings
    var isDesktopSite by remember { mutableStateOf(false) }
    var searchEngine by remember { mutableStateOf(prefs.getString("search_engine", "Google") ?: "Google") }
    var enableFloatingAssistant by remember { mutableStateOf(prefs.getBoolean("enable_floating_assistant", true)) }

    // Floating Assistant Drag Position inside WebBrowserScreen only
    var fabOffsetX by remember { mutableFloatStateOf(0f) }
    var fabOffsetY by remember { mutableFloatStateOf(0f) }
    var isFabInitialized by remember { mutableStateOf(false) }

    // Detected Media on the current webpage
    val detectedMediaList = remember { mutableStateListOf<DetectedMediaSource>() }

    // Active Downloads
    val activeDownloads = remember { mutableStateListOf<DownloadTask>() }

    // Sheets & Dialogs
    var showTabSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showMediaSheet by remember { mutableStateOf(false) }
    var showVpnDialog by remember { mutableStateOf(false) }
    var showTorDialog by remember { mutableStateOf(false) }

    // Hamburger Menu & History Management States
    var showBrowserMenuSheet by remember { mutableStateOf(false) }
    var showHistoryAndPrivacySheet by remember { mutableStateOf(false) }
    var showCloseAllTabsDialog by remember { mutableStateOf(false) }
    var showClearAllBrowsingDataDialog by remember { mutableStateOf(false) }

    // Dialog for editing filename before download
    var pendingDownloadSource by remember { mutableStateOf<DetectedMediaSource?>(null) }
    var pendingDownloadFilename by remember { mutableStateOf("") }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearHistory()
                    removeAllViews()
                    destroy()
                }
            } catch (_: Exception) {}
            webViewRef = null
        }
    }

    val bookmarks = remember {
        mutableStateListOf(
            BookmarkItem("Google", "https://www.google.com"),
            BookmarkItem("Wikipedia", "https://www.wikipedia.org"),
            BookmarkItem("Archive.org", "https://archive.org"),
            BookmarkItem("GitHub", "https://github.com")
        )
    }
    val history = remember { mutableStateListOf<String>() }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            when (searchEngine) {
                "DuckDuckGo" -> "https://duckduckgo.com/?q=${Uri.encode(trimmed)}"
                "Bing" -> "https://www.bing.com/search?q=${Uri.encode(trimmed)}"
                else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
            }
        }
    }

    fun navigateTo(url: String) {
        val trimmed = url.trim()
        val isDirectUrl = trimmed.startsWith("http://") || trimmed.startsWith("https://") || (trimmed.contains(".") && !trimmed.contains(" "))
        if (!isDirectUrl && trimmed.isNotBlank() && !isIncognito) {
            BrowserHistoryManager.addSearchQuery(context, trimmed)
        }
        val norm = normalizeUrl(url)
        urlInput = norm
        isHttps = norm.startsWith("https://")
        if (!isIncognito && !history.contains(norm)) {
            history.add(0, norm)
        }
        detectedMediaList.clear()
        webViewRef?.loadUrl(norm)
    }

    fun closeAllTabs() {
        tabs.forEach { tab ->
            if (tab.url.isNotBlank() && tab.url != "about:blank") {
                BrowserHistoryManager.addRecentlyClosedTab(context, tab)
            }
        }
        tabs.clear()
        val freshTab = BrowserTab(
            id = "tab_${System.currentTimeMillis()}",
            title = "Google",
            url = "https://www.google.com"
        )
        tabs.add(freshTab)
        currentTabIndex = 0
        navigateTo("https://www.google.com")
        scope.launch {
            snackbarHostState.showSnackbar("All tabs closed. Reset to clean tab.")
        }
    }

    fun restoreTab(closedTab: RecentlyClosedTab) {
        val restored = BrowserTab(
            id = "tab_${System.currentTimeMillis()}",
            title = closedTab.title,
            url = closedTab.url
        )
        tabs.add(restored)
        currentTabIndex = tabs.size - 1
        BrowserHistoryManager.removeRecentlyClosedTab(context, closedTab.id)
        navigateTo(closedTab.url)
        scope.launch {
            snackbarHostState.showSnackbar("Restored tab: ${closedTab.title}")
        }
    }

    // Inspect page media URLs safely
    fun registerDetectedMedia(url: String, mimeType: String = "") {
        if (url.isBlank() || url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("javascript:")) return
        if (detectedMediaList.any { it.url == url }) return

        val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
        val isVideo = ext in listOf("mp4", "mkv", "webm", "avi", "mov", "m4v") || mimeType.startsWith("video/")
        val isAudio = ext in listOf("mp3", "m4a", "flac", "wav", "ogg", "aac") || mimeType.startsWith("audio/")
        val isArchive = ext in listOf("zip", "rar", "7z", "tar", "gz") || mimeType.contains("zip") || mimeType.contains("compressed")
        val isDoc = ext in listOf("pdf", "doc", "docx", "txt", "apk")

        if (isVideo || isAudio || isArchive || isDoc) {
            val filename = URLUtil.guessFileName(url, null, mimeType).ifBlank { "media_$ext" }
            val mediaTitle = if (pageTitle.isNotBlank() && pageTitle != "Web") "$pageTitle ($ext)" else filename
            val resolution = if (isVideo && (url.contains("1080") || url.contains("720") || url.contains("480"))) {
                when {
                    url.contains("1080") -> "1080p"
                    url.contains("720") -> "720p"
                    url.contains("480") -> "480p"
                    else -> null
                }
            } else null

            detectedMediaList.add(
                DetectedMediaSource(
                    url = url,
                    title = mediaTitle,
                    mimeType = if (mimeType.isNotBlank()) mimeType else "application/octet-stream",
                    sizeBytes = 0L,
                    resolution = resolution
                )
            )
        }
    }

    val downloadEngine = remember { DownloadAccelerationEngine.getInstance(context) }
    val allDownloads by downloadEngine.tasksFlow.collectAsState()

    fun startDownload(source: DetectedMediaSource, customName: String) {
        val finalName = if (customName.isNotBlank()) customName else URLUtil.guessFileName(source.url, null, source.mimeType)
        val userAgent = webViewRef?.settings?.userAgentString
        // Immediately start download on background IO dispatcher without blocking UI thread
        scope.launch(Dispatchers.IO) {
            downloadEngine.enqueueDownload(
                url = source.url,
                suggestedFileName = finalName,
                userAgent = userAgent
            )
        }
        // Show snackbar feedback concurrently
        scope.launch {
            snackbarHostState.showSnackbar("Download queued: $finalName")
        }
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                if (isIncognito) {
                    Surface(
                        color = Color(0xFF202124),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Incognito Mode — browsing history and persistent cookies not stored", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { navigateTo(urlInput) }),
                            leadingIcon = {
                                IconButton(onClick = { showSecurityDialog = true }) {
                                    Icon(
                                        imageVector = if (isHttps) Icons.Default.Lock else Icons.Default.Info,
                                        contentDescription = "Connection Info",
                                        tint = if (isHttps) Color(0xFF1E8E3E) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isLoading) {
                                    IconButton(onClick = { webViewRef?.stopLoading() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = { webViewRef?.reload() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("browser_url_input")
                        )

                        IconButton(
                            onClick = { showBrowserMenuSheet = true },
                            modifier = Modifier.testTag("browser_hamburger_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Browser Menu")
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                        enabled = webViewRef?.canGoBack() == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    IconButton(
                        onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                        enabled = webViewRef?.canGoForward() == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }

                    IconButton(onClick = { navigateTo("https://www.google.com") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }

                    BadgedBox(badge = { Badge { Text("${tabs.size}") } }) {
                        IconButton(onClick = { showTabSheet = true }) {
                            Icon(Icons.Default.Tab, contentDescription = "Tabs")
                        }
                    }

                    IconButton(onClick = { showBookmarksSheet = true }) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks")
                    }

                    BadgedBox(badge = {
                        val activeCount = activeDownloads.count { it.status == DownloadTaskStatus.DOWNLOADING }
                        if (activeCount > 0) {
                            Badge { Text("$activeCount") }
                        }
                    }) {
                        IconButton(onClick = { showDownloadsSheet = true }) {
                            Icon(Icons.Default.Download, contentDescription = "Downloads")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // BoxWithConstraints encapsulates the browser view and in-browser floating assistant
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("web_browser_container")
        ) {
            val parentWidthPx = with(density) { maxWidth.toPx() }
            val parentHeightPx = with(density) { maxHeight.toPx() }
            val fabSizePx = with(density) { 56.dp.toPx() }

            // Initialize FAB position to bottom right of webview content area
            LaunchedEffect(maxWidth, maxHeight) {
                if (!isFabInitialized && parentWidthPx > 0 && parentHeightPx > 0) {
                    fabOffsetX = parentWidthPx - fabSizePx - with(density) { 20.dp.toPx() }
                    fabOffsetY = parentHeightPx - fabSizePx - with(density) { 24.dp.toPx() }
                    isFabInitialized = true
                }
            }

            // WebView
            AndroidView(
                factory = { ctx ->
                    BrowserHistoryManager.cleanupCorruptVariationsSeed(ctx)
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    urlInput = it
                                    isHttps = it.startsWith("https://")
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let {
                                    urlInput = it
                                    isHttps = it.startsWith("https://")
                                    if (!isIncognito && !history.contains(it)) history.add(0, it)
                                    val currentTitle = view?.title ?: pageTitle
                                    if (!isIncognito && it.isNotBlank() && it != "about:blank") {
                                        BrowserHistoryManager.addBrowsingHistory(context, currentTitle, it)
                                    }
                                }
                                pageTitle = view?.title ?: "Web"

                                // Check page for media tags
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var urls = [];
                                        var vids = document.getElementsByTagName('video');
                                        for(var i=0; i<vids.length; i++) {
                                            if(vids[i].src) urls.push(vids[i].src);
                                            var sources = vids[i].getElementsByTagName('source');
                                            for(var j=0; j<sources.length; j++) {
                                                if(sources[j].src) urls.push(sources[j].src);
                                            }
                                        }
                                        var auds = document.getElementsByTagName('audio');
                                        for(var i=0; i<auds.length; i++) {
                                            if(auds[i].src) urls.push(auds[i].src);
                                        }
                                        return JSON.stringify(urls);
                                    })();
                                    """.trimIndent()
                                ) { res ->
                                    if (!res.isNullOrBlank() && res != "null") {
                                        try {
                                            val clean = res.trim('"', '\\', ' ')
                                            if (clean.startsWith("[")) {
                                                val arr = org.json.JSONArray(clean)
                                                for (i in 0 until arr.length()) {
                                                    registerDetectedMedia(arr.getString(i))
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                }
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                }
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.cancel()
                                isLoading = false
                            }

                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                url?.let { registerDetectedMedia(it) }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                title?.let {
                                    pageTitle = it
                                    if (currentTabIndex in tabs.indices) {
                                        tabs[currentTabIndex] = tabs[currentTabIndex].copy(title = it)
                                    }
                                }
                            }
                        }

                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            pendingDownloadSource = DetectedMediaSource(
                                url = url,
                                title = filename,
                                mimeType = mimetype ?: "application/octet-stream",
                                sizeBytes = contentLength
                            )
                            pendingDownloadFilename = filename
                        }

                        loadUrl("https://www.google.com")
                        webViewRef = this
                    }
                },
                update = {
                    webViewRef = it
                    it.settings.userAgentString = if (isDesktopSite) {
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36"
                    } else null
                },
                onRelease = { wv ->
                    try {
                        wv.stopLoading()
                        wv.loadUrl("about:blank")
                        wv.clearHistory()
                        wv.removeAllViews()
                        wv.destroy()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )

            // FLOATING DOWNLOAD ASSISTANT (VISIBLE ONLY INSIDE WEB SCREEN)
            if (enableFloatingAssistant) {
                val mediaCount = detectedMediaList.size
                Box(
                    modifier = Modifier
                        .offset { IntOffset(fabOffsetX.roundToInt(), fabOffsetY.roundToInt()) }
                        .size(56.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(16f, parentWidthPx - fabSizePx - 16f)
                                fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(16f, parentHeightPx - fabSizePx - 16f)
                            }
                        }
                        .clip(CircleShape)
                        .background(if (mediaCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showMediaSheet = true }
                        .testTag("in_browser_floating_download_assistant"),
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (mediaCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("$mediaCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download Assistant",
                            tint = if (mediaCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    // Detected Media Bottom Sheet
    if (showMediaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Download Assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            if (detectedMediaList.isEmpty()) "No media detected on current page" else "${detectedMediaList.size} media sources detected",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = { showMediaSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (detectedMediaList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No downloadable audio, video, or files found", color = Color.Gray, fontSize = 13.sp)
                            Text("Browse to a video or file download page to capture media", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedMediaList) { media ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(media.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(media.mimeType, fontSize = 10.sp, color = Color.Gray)
                                            media.resolution?.let {
                                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                                    Text(it, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            showMediaSheet = false
                                            pendingDownloadSource = media
                                            pendingDownloadFilename = URLUtil.guessFileName(media.url, null, media.mimeType)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Download", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filename Confirmation Dialog before Download
    if (pendingDownloadSource != null) {
        val source = pendingDownloadSource!!
        AlertDialog(
            onDismissRequest = { pendingDownloadSource = null },
            icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Start Download") },
            text = {
                Column {
                    Text("Save to: Downloads/GMK_Downloads/", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pendingDownloadFilename,
                        onValueChange = { pendingDownloadFilename = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val src = pendingDownloadSource!!
                    val name = pendingDownloadFilename.trim()
                    pendingDownloadSource = null
                    startDownload(src, name)
                }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownloadSource = null }) { Text("Cancel") }
            }
        )
    }

    // Browser Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Web Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Search engine selection
                    Text("Search Engine", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Google", "DuckDuckGo", "Bing").forEach { se ->
                            OutlinedButton(
                                onClick = {
                                    searchEngine = se
                                    prefs.edit().putString("search_engine", se).apply()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (searchEngine == se) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(se, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    HorizontalDivider()

                    // Floating Download Assistant Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Download Assistant", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Shows draggable download button on web pages only", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = enableFloatingAssistant,
                            onCheckedChange = {
                                enableFloatingAssistant = it
                                prefs.edit().putBoolean("enable_floating_assistant", it).apply()
                            }
                        )
                    }

                    // Desktop Site Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Desktop Site", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Request desktop version of websites", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isDesktopSite,
                            onCheckedChange = {
                                isDesktopSite = it
                                webViewRef?.reload()
                            }
                        )
                    }

                    HorizontalDivider()

                    // Privacy & Security options
                    Text("Privacy & Security", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                showSettingsDialog = false
                                showVpnDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VpnLock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VPN", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                showSettingsDialog = false
                                showTorDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tor Proxy", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showSettingsDialog = false
                                showHistoryAndPrivacySheet = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("History", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                showSettingsDialog = false
                                showClearAllBrowsingDataDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Data", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) { Text("Done") }
            }
        )
    }

    // Browser Hamburger Menu Sheet (Navigation, Privacy & Security, Settings)
    if (showBrowserMenuSheet) {
        BrowserMenuSheet(
            onDismiss = { showBrowserMenuSheet = false },
            onNewTab = {
                val newTab = BrowserTab(
                    id = "tab_${System.currentTimeMillis()}",
                    title = "Google",
                    url = "https://www.google.com"
                )
                tabs.add(newTab)
                currentTabIndex = tabs.size - 1
                navigateTo("https://www.google.com")
            },
            onOpenTabs = { showTabSheet = true },
            onOpenBookmarks = { showBookmarksSheet = true },
            onOpenDownloads = { showDownloadsSheet = true },
            isIncognito = isIncognito,
            onToggleIncognito = { enabled ->
                isIncognito = enabled
                if (enabled) {
                    CookieManager.getInstance().removeSessionCookies(null)
                }
            },
            onOpenVpn = { showVpnDialog = true },
            onOpenTor = { showTorDialog = true },
            onOpenHistoryAndPrivacy = { showHistoryAndPrivacySheet = true },
            onOpenClearBrowsingData = { showClearAllBrowsingDataDialog = true },
            searchEngine = searchEngine,
            onSelectSearchEngine = {
                searchEngine = it
                prefs.edit().putString("search_engine", it).apply()
            },
            isDesktopSite = isDesktopSite,
            onToggleDesktopSite = {
                isDesktopSite = it
                webViewRef?.reload()
            },
            enableFloatingAssistant = enableFloatingAssistant,
            onToggleFloatingAssistant = {
                enableFloatingAssistant = it
                prefs.edit().putBoolean("enable_floating_assistant", it).apply()
            },
            onOpenSettings = { showSettingsDialog = true },
            tabCount = tabs.size,
            bookmarkCount = bookmarks.size,
            activeDownloadCount = activeDownloads.count { it.status == DownloadTaskStatus.DOWNLOADING }
        )
    }

    // Complete History & Privacy Sheet
    if (showHistoryAndPrivacySheet) {
        BrowserHistoryAndPrivacySheet(
            onDismiss = { showHistoryAndPrivacySheet = false },
            onNavigateToUrl = { target ->
                navigateTo(target)
            },
            onCloseAllTabsRequested = {
                showCloseAllTabsDialog = true
            },
            onRestoreTab = { closed ->
                restoreTab(closed)
            },
            onClearCookies = {
                BrowserHistoryManager.clearCookies {
                    scope.launch {
                        snackbarHostState.showSnackbar("Cookies and site data cleared")
                    }
                }
            },
            onClearCache = {
                BrowserHistoryManager.clearCache(webViewRef)
                scope.launch {
                    snackbarHostState.showSnackbar("Cached images and files cleared")
                }
            },
            onOpenClearAllBrowsingData = {
                showClearAllBrowsingDataDialog = true
            },
            openTabsCount = tabs.size
        )
    }

    // Close All Tabs Confirmation Dialog
    if (showCloseAllTabsDialog) {
        CloseAllTabsDialog(
            tabCount = tabs.size,
            onConfirm = {
                showCloseAllTabsDialog = false
                closeAllTabs()
            },
            onDismiss = { showCloseAllTabsDialog = false }
        )
    }

    // Clear All Browsing Data Dialog (Selective Checkboxes)
    if (showClearAllBrowsingDataDialog) {
        ClearAllBrowsingDataDialog(
            onConfirm = { options ->
                showClearAllBrowsingDataDialog = false
                if (options.closeOpenTabs) {
                    closeAllTabs()
                }
                BrowserHistoryManager.clearBrowsingData(context, options, webViewRef) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Selected browsing data cleared successfully")
                    }
                }
            },
            onDismiss = { showClearAllBrowsingDataDialog = false }
        )
    }

    // VPN Information Dialog
    if (showVpnDialog) {
        AlertDialog(
            onDismissRequest = { showVpnDialog = false },
            icon = { Icon(Icons.Default.VpnLock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("VPN Connection") },
            text = {
                Column {
                    Text(
                        "An encrypted VPN tunnel requires an active Android VPN Service profile or system configuration (e.g. WireGuard, OpenVPN, or your organization's VPN).",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Status: Device Network Connection Active", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("To route traffic through a VPN, please configure your preferred VPN in Android Settings > Network & internet > VPN.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showVpnDialog = false }) { Text("Got It") }
            }
        )
    }

    // Tor Information Dialog
    if (showTorDialog) {
        AlertDialog(
            onDismissRequest = { showTorDialog = false },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Tor Network Configuration") },
            text = {
                Column {
                    Text(
                        "Tor routing requires a local Tor SOCKS5 proxy running on your device (such as Orbot on 127.0.0.1:9050).",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Standard Proxy: 127.0.0.1:9050 (Orbot)", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            Text("When Orbot is active on your device, all HTTP requests can route securely through the onion network.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTorDialog = false }) { Text("Done") }
            }
        )
    }

    // Connection Security Dialog
    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            icon = { Icon(if (isHttps) Icons.Default.Lock else Icons.Default.Info, contentDescription = null, tint = if (isHttps) Color(0xFF1E8E3E) else Color.Gray) },
            title = { Text(if (isHttps) "Connection is Secure" else "Connection Not Secure") },
            text = {
                Column {
                    Text("URL: $urlInput", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (isHttps) "Your information (e.g. passwords or credit card numbers) is private when sent to this site, protected by TLS encryption."
                        else "This website does not use HTTPS. Information sent over this connection may be visible to others.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSecurityDialog = false }) { Text("OK") }
            }
        )
    }

    // Tabs Sheet
    if (showTabSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTabSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tabs (${tabs.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Button(onClick = {
                        if (tabs.size < 15) {
                            val newTab = BrowserTab(id = "tab_${System.currentTimeMillis()}", title = "Google", url = "https://www.google.com")
                            tabs.add(newTab)
                            currentTabIndex = tabs.size - 1
                            navigateTo("https://www.google.com")
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Maximum tab limit (15) reached. Close unused tabs to save memory.")
                            }
                        }
                        showTabSheet = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Tab")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    items(tabs) { tab ->
                        val isSelected = tabs.indexOf(tab) == currentTabIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newIdx = tabs.indexOf(tab)
                                    if (newIdx != currentTabIndex || webViewRef?.url != tab.url) {
                                        currentTabIndex = newIdx
                                        navigateTo(tab.url)
                                    }
                                    showTabSheet = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tab, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tab.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (tabs.size > 1) {
                                IconButton(onClick = {
                                    val idx = tabs.indexOf(tab)
                                    tabs.remove(tab)
                                    if (currentTabIndex >= tabs.size) currentTabIndex = tabs.size - 1
                                    navigateTo(tabs[currentTabIndex].url)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close tab")
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // Bookmarks Sheet
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bookmarks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    TextButton(onClick = {
                        bookmarks.add(BookmarkItem(pageTitle, urlInput))
                        scope.launch { snackbarHostState.showSnackbar("Bookmarked page") }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bookmark Current")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    items(bookmarks) { bm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateTo(bm.url)
                                    showBookmarksSheet = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bm.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(bm.url, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // Chrome-like Downloads Sheet with Categorized Tabs, Real-time Speed & Turbo Range Status
    if (showDownloadsSheet) {
        ChromeDownloadsSheet(
            engine = downloadEngine,
            userAgent = webViewRef?.settings?.userAgentString,
            onOpenDownloadedMedia = { media ->
                showDownloadsSheet = false
                onOpenDownloadedMedia(media)
            },
            onDismiss = { showDownloadsSheet = false }
        )
    }
}
