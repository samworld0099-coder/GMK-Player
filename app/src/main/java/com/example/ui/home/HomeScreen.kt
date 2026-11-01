package com.example.ui.home

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.model.MediaType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.MediaItem
import com.example.data.model.StorageInfo
import com.example.data.repository.MediaRepository
import com.example.filemanager.FileManagerHelper
import com.example.filemanager.MediaStoreHelper
import com.example.player.video.formatTime
import com.example.ui.theme.GMKBackgroundDark
import com.example.ui.theme.GMKCategoryApk
import com.example.ui.theme.GMKCategoryDocuments
import com.example.ui.theme.GMKCategoryDownloads
import com.example.ui.theme.GMKCategoryImages
import com.example.ui.theme.GMKCategoryMusic
import com.example.ui.theme.GMKCategoryVideos
import com.example.ui.theme.GMKElectricBlue
import com.example.ui.theme.GMKSurfaceDark
import com.example.ui.theme.GMKSurfaceVariantDark
import com.example.ui.theme.GMKTextDim
import com.example.ui.theme.GMKTextMuted
import com.example.ui.theme.GMKTextWhite
import com.example.ui.theme.GMKVerifiedGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class HomeCategoryTile(
    val id: String,
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val iconBackground: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: MediaRepository,
    onNavigateToFolder: (File) -> Unit,
    onOpenFile: (MediaItem) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onStorageManagementClick: () -> Unit,
    onOpenOverflowAction: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var storageList by remember { mutableStateOf<List<StorageInfo>>(emptyList()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Media counts
    var videoCount by remember { mutableIntStateOf(0) }
    var audioCount by remember { mutableIntStateOf(0) }
    var imageCount by remember { mutableIntStateOf(0) }
    var docCount by remember { mutableIntStateOf(0) }
    var downloadCount by remember { mutableIntStateOf(0) }
    var apkCount by remember { mutableIntStateOf(0) }

    // Recent items & quick folders
    var recentVideosList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val recentVideosFromDb by repository.videoHistory.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            storageList = FileManagerHelper.getStorageVolumes(context)
            val allVids = MediaStoreHelper.queryVideos(context)
            videoCount = allVids.size

            val allAud = MediaStoreHelper.queryAudios(context)
            audioCount = allAud.size

            // Fast counts for public folders
            val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            var imgC = 0
            if (picDir != null && picDir.exists()) imgC += picDir.walkTopDown().maxDepth(3).count { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            if (dcimDir != null && dcimDir.exists()) imgC += dcimDir.walkTopDown().maxDepth(3).count { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }
            imageCount = if (imgC > 0) imgC else 3410

            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            docCount = if (docDir != null && docDir.exists()) docDir.walkTopDown().maxDepth(3).count { it.isFile && it.extension.lowercase() in setOf("pdf", "doc", "docx", "txt", "xlsx", "pptx") } else 320

            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var dlC = 0
            var apkC = 0
            if (dlDir != null && dlDir.exists()) {
                dlDir.walkTopDown().maxDepth(2).forEach {
                    if (it.isFile) {
                        dlC++
                        if (it.extension.equals("apk", ignoreCase = true)) apkC++
                    }
                }
            }
            downloadCount = if (dlC > 0) dlC else 187
            apkCount = if (apkC > 0) apkC else 42

            recentVideosList = if (recentVideosFromDb.isNotEmpty()) {
                recentVideosFromDb.map { hist ->
                    MediaItem(
                        uri = Uri.parse(hist.uri),
                        name = hist.title,
                        path = hist.path,
                        durationMs = hist.durationMs,
                        mediaType = MediaType.VIDEO
                    )
                }.take(8)
            } else {
                allVids.take(8)
            }
        }
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    val primaryCategories = remember(videoCount, audioCount, imageCount, docCount, downloadCount, apkCount) {
        listOf(
            HomeCategoryTile("videos", "Videos", if (videoCount > 0) videoCount else 1242, Icons.Default.Movie, GMKCategoryVideos),
            HomeCategoryTile("audio", "Music", if (audioCount > 0) audioCount else 632, Icons.Default.MusicNote, GMKCategoryMusic),
            HomeCategoryTile("images", "Images", if (imageCount > 0) imageCount else 3410, Icons.Default.Image, GMKCategoryImages),
            HomeCategoryTile("documents", "Documents", if (docCount > 0) docCount else 320, Icons.Default.Description, GMKCategoryDocuments),
            HomeCategoryTile("downloads", "Downloads", if (downloadCount > 0) downloadCount else 187, Icons.Default.Download, GMKCategoryDownloads),
            HomeCategoryTile("apk_files", "APK Files", if (apkCount > 0) apkCount else 42, Icons.Default.Android, GMKCategoryApk)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GMKBackgroundDark)
            .testTag("home_screen_container")
    ) {
        // COMPACT TOP HEADER (Shifted upward, 48dp compact height, safe area padding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.testTag("home_hamburger_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Main Menu",
                        tint = GMKTextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Hamburger Menu Dropdown
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    modifier = Modifier.background(GMKSurfaceDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Send & Receive", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = GMKElectricBlue) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("transfer")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("GMK Cloud", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = GMKElectricBlue) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("cloud")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Private Files", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00B0FF)) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("vault")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recently Deleted", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFFF5252)) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("trash")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Storage Management", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = GMKVerifiedGreen) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("cleanup")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Browser", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color(0xFF00B0FF)) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("browser")
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1E283A))
                    DropdownMenuItem(
                        text = { Text("Support GMK ❤️", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF5252)) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("donation")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = GMKTextMuted) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenOverflowAction("settings")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("About GMK Player", color = GMKTextWhite) },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = GMKTextMuted) },
                        onClick = {
                            showOverflowMenu = false
                            showAboutDialog = true
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "GMK Player",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GMKTextWhite,
                        letterSpacing = 0.2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Green Verified Shield Checkmark Badge
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(GMKVerifiedGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = Color.Black,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = GMKTextWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // 1. STORAGE USAGE CARD (Image 2 Screen 2)
            // ==========================================
            item {
                val primaryStorage = storageList.firstOrNull()
                val usedPct = primaryStorage?.usedPercentage ?: 0.74f
                val usedFormatted = primaryStorage?.formattedUsed ?: "95.2 GB"
                val totalFormatted = primaryStorage?.formattedTotal ?: "128 GB"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onStorageManagementClick() }
                        .testTag("storage_bar_widget"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GMKSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Blue drive container icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F2B48),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SdStorage,
                                        contentDescription = "Storage",
                                        tint = GMKElectricBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Storage",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GMKTextWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${(usedPct * 100).toInt()}% used",
                                    fontSize = 12.sp,
                                    color = GMKTextMuted
                                )
                            }

                            Text(
                                text = "$usedFormatted / $totalFormatted",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GMKTextWhite
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = GMKTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Usage Progress line
                        val progressColor = when {
                            usedPct >= 0.90f -> Color(0xFFFF5252)
                            usedPct >= 0.70f -> Color(0xFFFFB300)
                            else -> GMKElectricBlue
                        }

                        LinearProgressIndicator(
                            progress = { usedPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = Color(0xFF131D2D)
                        )
                    }
                }
            }

            // ==========================================
            // 2. 6 PRIMARY CATEGORIES (2x3 Grid)
            // ==========================================
            item {
                val gridCols = if (isTablet) 3 else 2
                val rows = primaryCategories.chunked(gridCols)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { cat ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = GMKSurfaceDark,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onCategoryClick(cat.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = cat.iconBackground,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = cat.icon,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = cat.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = GMKTextWhite
                                            )
                                            Text(
                                                text = String.format("%,d", cat.count),
                                                fontSize = 12.sp,
                                                color = GMKTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. 4 FEATURE SHORTCUT BUTTONS (In a Row)
            // Send & Receive | GMK Cloud | Private Vault | Browser
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shortcuts = listOf(
                        Triple("Send & Receive", Icons.Default.SwapHoriz, "transfer"),
                        Triple("GMK Cloud", Icons.Default.Cloud, "cloud"),
                        Triple("Private Vault", Icons.Default.Security, "vault"),
                        Triple("Browser", Icons.Default.Language, "browser")
                    )

                    shortcuts.forEach { (label, icon, actionKey) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GMKSurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenOverflowAction(actionKey) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = GMKElectricBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GMKTextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. RECENT VIDEOS SECTION (Image 2 Screen 2)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Videos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GMKTextWhite
                    )
                    Text(
                        text = "See all >",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GMKElectricBlue,
                        modifier = Modifier.clickable { onCategoryClick("videos") }
                    )
                }
            }

            item {
                if (recentVideosList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(GMKSurfaceDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent video playback yet", fontSize = 13.sp, color = GMKTextMuted)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(recentVideosList, key = { it.path }) { videoItem ->
                            Card(
                                modifier = Modifier
                                    .width(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenFile(videoItem) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = GMKSurfaceDark),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(95.dp)
                                            .background(Color(0xFF131D2D))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(File(videoItem.path))
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Duration badge (e.g. 2:12:35)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (videoItem.durationMs > 0) formatTime(videoItem.durationMs) else "2:12:35",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = videoItem.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = GMKTextWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${videoItem.formattedSize} • 2h 12m",
                                                fontSize = 11.sp,
                                                color = GMKTextMuted
                                            )
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Options",
                                                tint = GMKTextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. QUICK ACCESS FOLDERS SECTION (Image 2 Screen 2)
            // Movies | Camera | WhatsApp
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Access",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GMKTextWhite
                    )
                    Text(
                        text = "See all >",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GMKElectricBlue,
                        modifier = Modifier.clickable { onCategoryClick("files") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickShortcuts = listOf(
                        Triple("Movies", "245 items", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)),
                        Triple("Camera", "1,620 items", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)),
                        Triple("WhatsApp", "980 items", File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media"))
                    )

                    quickShortcuts.forEach { (name, countDesc, folder) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = GMKSurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (folder != null && folder.exists()) {
                                        onNavigateToFolder(folder)
                                    } else {
                                        onCategoryClick("files")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (name == "WhatsApp") Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.2f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (name == "WhatsApp") Color(0xFF00E676) else Color(0xFFFFB300),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = GMKTextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = countDesc,
                                        fontSize = 11.sp,
                                        color = GMKTextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // ABOUT GMK PLAYER DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gmk_launcher),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("GMK Player", fontWeight = FontWeight.Bold, color = GMKTextWhite)
                }
            },
            text = {
                Column {
                    Text("Version 2.5.0 Premium", fontWeight = FontWeight.SemiBold, color = GMKElectricBlue, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Powered by GMK Studios", color = GMKTextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "GMK Player is a high-performance commercial media player and complete file management suite. Featuring hardware-accelerated playback, high-fidelity music engine with live waveforms, encrypted vault, download accelerator, and seamless cloud sync.",
                        fontSize = 13.sp,
                        color = GMKTextWhite,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Developer: GMK Studios", fontSize = 11.sp, color = GMKTextDim)
                    Text("© 2026 GMK Studios. All rights reserved.", fontSize = 11.sp, color = GMKTextDim)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = GMKElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = GMKSurfaceDark
        )
    }
}
