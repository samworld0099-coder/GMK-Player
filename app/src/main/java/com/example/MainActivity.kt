package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppDatabase
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.filemanager.FileManagerHelper
import com.example.filemanager.MediaStoreHelper
import com.example.player.audio.AudioPlayerViewModel
import com.example.player.audio.MiniPlayerBar
import com.example.player.audio.MusicScreen
import com.example.player.video.VideoPlayerScreen
import com.example.player.video.VideoPlayerViewModel
import com.example.ui.archive.ArchiveBrowserScreen
import com.example.ui.archive.ArchivesListScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.files.FileBrowserScreen
import com.example.ui.home.HomeScreen
import com.example.ui.recent.RecentScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.videos.VideosScreen
import com.example.cloud.GMKCloudScreen
import com.example.ui.splash.SplashScreen
import android.content.Context
import com.example.browser.WebBrowserScreen
import com.example.cleanup.StorageCleanupScreen
import com.example.contact.ContactUsScreen
import com.example.donation.DonationManager
import com.example.donation.DonationScreen
import com.example.donation.RoyalBlueDonationPopup
import com.example.feedback.FeedbackScreen
import com.example.transfer.SendReceiveScreen
import com.example.ui.trash.RecentlyDeletedScreen
import com.example.vault.PrivateVaultScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.GMKMediaPlayerTheme
import com.example.ui.theme.GMKBackgroundDark
import com.example.ui.theme.GMKSurfaceDark
import com.example.ui.theme.GMKElectricBlue
import com.example.ui.theme.GMKTextMuted
import com.example.ui.theme.GMKTextWhite
import com.example.ui.viewer.DocumentViewerScreen
import kotlinx.coroutines.launch
import java.io.File

sealed class ActiveScreen {
    object MainNavigation : ActiveScreen()
    data class VideoPlayer(val uri: Uri, val title: String) : ActiveScreen()
    data class ArchiveBrowser(val file: File) : ActiveScreen()
    data class DocumentViewer(val item: MediaItem) : ActiveScreen()
    object Search : ActiveScreen()
    data class SendReceive(val initialFiles: List<File> = emptyList()) : ActiveScreen()
    object PrivateVault : ActiveScreen()
    object RecentlyDeleted : ActiveScreen()
    object StorageCleanup : ActiveScreen()
    object WebBrowser : ActiveScreen()
    object Donation : ActiveScreen()
    object Feedback : ActiveScreen()
    object ContactUs : ActiveScreen()
    object Settings : ActiveScreen()
}

class MainActivity : ComponentActivity() {

    private val videoViewModel: VideoPlayerViewModel by viewModels()
    private val audioViewModel: AudioPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.browser.BrowserHistoryManager.cleanupCorruptVariationsSeed(this)

        val db = AppDatabase.getInstance(this)
        val repository = MediaRepository(db)
        val prefs = getSharedPreferences("gmk_settings", Context.MODE_PRIVATE)

        setContent {
            val initialTheme = remember {
                val saved = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)
                try {
                    AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
                } catch (_: Exception) {
                    AppThemeMode.SYSTEM
                }
            }
            var currentTheme by remember { mutableStateOf(initialTheme) }

            GMKMediaPlayerTheme(themeMode = currentTheme) {
                MainAppContent(
                    repository = repository,
                    videoViewModel = videoViewModel,
                    audioViewModel = audioViewModel,
                    initialIntent = intent,
                    currentTheme = currentTheme,
                    onThemeChanged = { newTheme ->
                        currentTheme = newTheme
                        prefs.edit().putString("theme_mode", newTheme.name).apply()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    repository: MediaRepository,
    videoViewModel: VideoPlayerViewModel,
    audioViewModel: AudioPlayerViewModel,
    initialIntent: Intent?,
    currentTheme: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeChanged: (AppThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSplashScreen by remember { mutableStateOf(true) }

    if (showSplashScreen) {
        SplashScreen(onFinished = { showSplashScreen = false })
        return
    }

    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.MainNavigation) }
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var showDonationPopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (DonationManager.shouldShowDonationPopup(context)) {
            kotlinx.coroutines.delay(2000)
            if (activeScreen == ActiveScreen.MainNavigation) {
                showDonationPopup = true
                DonationManager.markPopupShown(context)
            }
        }
    }

    if (showDonationPopup && activeScreen == ActiveScreen.MainNavigation) {
        RoyalBlueDonationPopup(
            onDonateNow = {
                showDonationPopup = false
                activeScreen = ActiveScreen.Donation
            },
            onDismiss = {
                showDonationPopup = false
                DonationManager.markPopupDismissed(context)
            }
        )
    }

    var currentFolder by remember {
        mutableStateOf(Environment.getExternalStorageDirectory() ?: context.filesDir)
    }

    var allVideos by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allAudios by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var allArchives by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    // Request permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Reload media regardless of grant (falls back to app directories & SAF)
        scope.launch {
            allVideos = MediaStoreHelper.queryVideos(context)
            allAudios = MediaStoreHelper.queryAudios(context)
            allArchives = MediaStoreHelper.queryArchives(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
        allVideos = MediaStoreHelper.queryVideos(context)
        allAudios = MediaStoreHelper.queryAudios(context)
        allArchives = MediaStoreHelper.queryArchives(context)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                scope.launch {
                    allVideos = MediaStoreHelper.queryVideos(context)
                }
            }
        }
        try {
            context.contentResolver.registerContentObserver(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {}

        onDispose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }

    // Handle incoming VIEW / SEND intents from outside
    LaunchedEffect(initialIntent) {
        initialIntent?.data?.let { uri ->
            val name = getFileNameFromUri(context, uri)
            val ext = name.substringAfterLast('.', "")
            val mediaType = FileManagerHelper.determineMediaType(ext, false)
            when {
                mediaType == MediaType.VIDEO -> {
                    activeScreen = ActiveScreen.VideoPlayer(uri, name)
                }
                mediaType == MediaType.AUDIO -> {
                    audioViewModel.playTrack(
                        MediaItem(
                            uri = uri,
                            name = name,
                            path = uri.path ?: "",
                            mediaType = MediaType.AUDIO,
                            extension = ext
                        )
                    )
                    selectedNavIndex = 3 // Switch to Music
                }
                FileManagerHelper.isArchiveExtension(ext) && uri.path != null -> {
                    activeScreen = ActiveScreen.ArchiveBrowser(File(uri.path!!))
                }
            }
        }
    }

    fun openMediaItem(item: MediaItem) {
        when {
            item.isDirectory -> {
                currentFolder = File(item.path)
                selectedNavIndex = 1 // Switch to Browse
            }
            item.mediaType == MediaType.VIDEO -> {
                activeScreen = ActiveScreen.VideoPlayer(item.uri, item.name)
            }
            item.mediaType == MediaType.AUDIO -> {
                audioViewModel.playTrack(item, allAudios)
            }
            item.isArchive || item.mediaType == MediaType.ARCHIVE -> {
                val file = File(item.path)
                scope.launch {
                    repository.recordArchiveAccess(
                        uri = item.uri.toString(),
                        name = item.name,
                        path = item.path,
                        format = item.extension.uppercase(),
                        entryCount = 0,
                        size = item.size
                    )
                }
                activeScreen = ActiveScreen.ArchiveBrowser(file)
            }
            item.mediaType == MediaType.IMAGE || item.mediaType == MediaType.DOCUMENT -> {
                activeScreen = ActiveScreen.DocumentViewer(item)
            }
            else -> {
                activeScreen = ActiveScreen.DocumentViewer(item)
            }
        }
    }

    when (val screen = activeScreen) {
        is ActiveScreen.VideoPlayer -> {
            VideoPlayerScreen(
                videoUri = screen.uri,
                title = screen.title,
                viewModel = videoViewModel,
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.ArchiveBrowser -> {
            ArchiveBrowserScreen(
                archiveFile = screen.file,
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.DocumentViewer -> {
            DocumentViewerScreen(
                item = screen.item,
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.Search -> {
            val allCombined = remember(allVideos, allAudios, allArchives) {
                allVideos + allAudios + allArchives
            }
            SearchScreen(
                allItems = allCombined,
                onOpenItem = { item ->
                    openMediaItem(item)
                },
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.SendReceive -> {
            SendReceiveScreen(
                initialPreselectedFiles = screen.initialFiles,
                onOpenFile = { openMediaItem(it) },
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.PrivateVault -> {
            PrivateVaultScreen(
                onOpenMediaItem = { openMediaItem(it) },
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.RecentlyDeleted -> {
            RecentlyDeletedScreen(
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.StorageCleanup -> {
            StorageCleanupScreen(
                onOpenFile = { openMediaItem(it) },
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.WebBrowser -> {
            WebBrowserScreen(
                onOpenDownloadedMedia = { openMediaItem(it) },
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.Donation -> {
            DonationScreen(
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.Feedback -> {
            FeedbackScreen(
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.ContactUs -> {
            ContactUsScreen(
                onOpenFeedback = { activeScreen = ActiveScreen.Feedback },
                onOpenDonation = { activeScreen = ActiveScreen.Donation },
                onBack = { activeScreen = ActiveScreen.MainNavigation }
            )
        }
        is ActiveScreen.Settings -> {
            SettingsScreen(
                repository = repository,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged,
                onOpenStorageCleanup = { activeScreen = ActiveScreen.StorageCleanup },
                onOpenDonation = { activeScreen = ActiveScreen.Donation },
                onOpenFeedback = { activeScreen = ActiveScreen.Feedback },
                onOpenContactUs = { activeScreen = ActiveScreen.ContactUs }
            )
        }
        is ActiveScreen.MainNavigation -> {
            BackHandler(enabled = selectedNavIndex != 0) {
                selectedNavIndex = 0
            }

            val navItems = listOf(
                "Home" to Icons.Default.Home,
                "Videos" to Icons.Default.Movie,
                "Music" to Icons.Default.MusicNote,
                "Cloud" to Icons.Default.Cloud,
                "Browser" to Icons.Default.Language,
                "Files" to Icons.Default.Folder
            )

            Scaffold(
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .background(GMKBackgroundDark)
                            .navigationBarsPadding()
                    ) {
                        // Persistent Mini Player when audio is loaded
                        val audioState by audioViewModel.uiState.collectAsState()
                        if (audioState.currentTrack != null && selectedNavIndex != 2) {
                            MiniPlayerBar(
                                track = audioState.currentTrack!!,
                                isPlaying = audioState.isPlaying,
                                onPlayPause = { audioViewModel.togglePlayPause() },
                                onNext = { audioViewModel.playNext() },
                                onClick = { audioViewModel.setExpandedPlayerVisible(true) }
                            )
                        }

                        // 6-destination Bottom Navigation Bar matching reference design: Home | Videos | Music | Cloud | Browser | Files
                        NavigationBar(
                            containerColor = GMKSurfaceDark,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("main_navigation_bar")
                        ) {
                            navItems.forEachIndexed { index, (label, icon) ->
                                val isSelected = selectedNavIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedNavIndex = index },
                                    alwaysShowLabel = true,
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isSelected) GMKElectricBlue else GMKTextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            color = if (isSelected) GMKElectricBlue else GMKTextMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = GMKElectricBlue.copy(alpha = 0.15f),
                                        selectedIconColor = GMKElectricBlue,
                                        selectedTextColor = GMKElectricBlue,
                                        unselectedIconColor = GMKTextMuted,
                                        unselectedTextColor = GMKTextMuted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedNavIndex) {
                        0 -> HomeScreen(
                            repository = repository,
                            onNavigateToFolder = { dir ->
                                currentFolder = dir
                                selectedNavIndex = 5
                            },
                            onOpenFile = { openMediaItem(it) },
                            onCategoryClick = { catId ->
                                when (catId) {
                                    "videos" -> selectedNavIndex = 1
                                    "audio" -> selectedNavIndex = 2
                                    "cloud" -> selectedNavIndex = 3
                                    "browser" -> selectedNavIndex = 4
                                    "files" -> selectedNavIndex = 5
                                    "images" -> {
                                        val pics = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                                        if (pics != null && pics.exists()) currentFolder = pics
                                        selectedNavIndex = 5
                                    }
                                    "documents" -> {
                                        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                                        if (docs != null && docs.exists()) currentFolder = docs
                                        selectedNavIndex = 5
                                    }
                                    "downloads" -> {
                                        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        if (dl != null && dl.exists()) currentFolder = dl
                                        selectedNavIndex = 5
                                    }
                                    "apk_files" -> {
                                        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        if (dl != null && dl.exists()) currentFolder = dl
                                        selectedNavIndex = 5
                                    }
                                    "large_files" -> activeScreen = ActiveScreen.StorageCleanup
                                    else -> selectedNavIndex = 5
                                }
                            },
                            onSearchClick = { activeScreen = ActiveScreen.Search },
                            onStorageManagementClick = { activeScreen = ActiveScreen.StorageCleanup },
                            onOpenOverflowAction = { action ->
                                when (action) {
                                    "transfer" -> activeScreen = ActiveScreen.SendReceive()
                                    "cloud" -> selectedNavIndex = 3
                                    "vault" -> activeScreen = ActiveScreen.PrivateVault
                                    "trash" -> activeScreen = ActiveScreen.RecentlyDeleted
                                    "cleanup" -> activeScreen = ActiveScreen.StorageCleanup
                                    "browser" -> selectedNavIndex = 4
                                    "donation" -> activeScreen = ActiveScreen.Donation
                                    "settings" -> activeScreen = ActiveScreen.Settings
                                }
                            }
                        )
                        1 -> VideosScreen(
                            videos = allVideos,
                            onVideoClick = { openMediaItem(it) },
                            onRefreshVideos = {
                                scope.launch {
                                    allVideos = MediaStoreHelper.queryVideos(context)
                                }
                            },
                            onSendVideos = { files ->
                                activeScreen = ActiveScreen.SendReceive(initialFiles = files)
                            },
                            onOpenSettings = {
                                activeScreen = ActiveScreen.Settings
                            }
                        )
                        2 -> MusicScreen(
                            songs = allAudios,
                            viewModel = audioViewModel,
                            onOpenSearch = { activeScreen = ActiveScreen.Search },
                            onOpenDrawer = { activeScreen = ActiveScreen.Settings }
                        )
                        3 -> GMKCloudScreen(
                            onOpenFile = { openMediaItem(it) },
                            onMenuClick = { activeScreen = ActiveScreen.Settings },
                            onSearchClick = { activeScreen = ActiveScreen.Search }
                        )
                        4 -> WebBrowserScreen(
                            onOpenDownloadedMedia = { openMediaItem(it) },
                            onBack = { selectedNavIndex = 0 }
                        )
                        5 -> FileBrowserScreen(
                            initialDirectory = currentFolder,
                            onOpenFile = { openMediaItem(it) },
                            onToggleFavorite = { scope.launch { repository.toggleFavorite(it) } },
                            onBack = { selectedNavIndex = 0 }
                        )
                    }
                }
            }
        }
    }
}

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = cursor.getString(idx)
                }
            }
        } catch (_: Exception) {}
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result ?: "media_file"
}
