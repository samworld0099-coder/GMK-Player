package com.example.ui.home

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.StorageInfo
import com.example.data.repository.MediaRepository
import com.example.filemanager.FileManagerHelper
import com.example.player.video.formatTime
import java.io.File

data class CategoryGridItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color
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
    var storageList by remember { mutableStateOf<List<StorageInfo>>(emptyList()) }
    var quickFolders by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val recentVideos by repository.videoHistory.collectAsState(initial = emptyList())
    val recentArchives by repository.recentArchives.collectAsState(initial = emptyList())

    // SAF file picker to manually open any file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected File"
            val ext = name.substringAfterLast('.', "")
            val mediaType = FileManagerHelper.determineMediaType(ext, false)
            onOpenFile(
                MediaItem(
                    uri = uri,
                    name = name,
                    path = uri.path ?: "",
                    mediaType = mediaType,
                    extension = ext,
                    isArchive = FileManagerHelper.isArchiveExtension(ext)
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        storageList = FileManagerHelper.getStorageVolumes(context)
        quickFolders = FileManagerHelper.getQuickFolders(context)
    }

    val categories = listOf(
        CategoryGridItem("images", "Images", Icons.Default.Image, Color(0xFF1A73E8)),
        CategoryGridItem("videos", "Videos", Icons.Default.Movie, Color(0xFFEA4335)),
        CategoryGridItem("audio", "Audio", Icons.Default.MusicNote, Color(0xFFF29900)),
        CategoryGridItem("documents", "Documents", Icons.Default.Description, Color(0xFF1E8E3E)),
        CategoryGridItem("downloads", "Downloads", Icons.Default.Download, Color(0xFF129EAF)),
        CategoryGridItem("archives", "Archives", Icons.Default.FolderZip, Color(0xFFFBBC04)),
        CategoryGridItem("large_files", "Large files", Icons.Default.CleaningServices, Color(0xFF5F6368)),
        CategoryGridItem("recent_files", "Recent files", Icons.Default.History, Color(0xFF80868B))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_container")
    ) {
        // TOP APP BAR (Google Files style)
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gmk_launcher),
                        contentDescription = "GMK Media Player Icon",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GMK Player",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("home_hamburger_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Main Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Send & Receive") },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("transfer")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Private Files") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF185ABC)) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("vault")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Deleted") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFD93025)) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("trash")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Storage Management") },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("cleanup")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Web") },
                            leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("browser")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Support GMK ❤️") },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFD32F2F)) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("donation")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOverflowAction("settings")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About GMK Player") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                showAboutDialog = true
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // STORAGE CARD — Royal Blue Container with White/Alert Usage Line
            item {
                val primaryStorage = storageList.firstOrNull()
                primaryStorage?.let { storage ->
                    val usedPct = storage.usedPercentage
                    // Dynamic progress indicator color:
                    // 0%–69%: WHITE, 70%–79%: YELLOW, 80%–89%: ORANGE, 90%–100%: RED
                    val progressColor = when {
                        usedPct >= 0.90f -> Color(0xFFF44336) // Red (90% - 100%)
                        usedPct >= 0.80f -> Color(0xFFFF9800) // Orange (80% - 89%)
                        usedPct >= 0.70f -> Color(0xFFFFEB3B) // Yellow (70% - 79%)
                        else -> Color.White                   // Pure White (0% - 69%)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onStorageManagementClick() }
                            .testTag("storage_bar_widget"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0D47A1) // Premium Royal Blue
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.20f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SdStorage,
                                            contentDescription = "Storage",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = storage.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${storage.formattedUsed} used of ${storage.formattedTotal} • ${storage.formattedFree} free",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.90f)
                                    )
                                }
                                Button(
                                    onClick = onStorageManagementClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0D47A1)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Clean up", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Storage Usage Progress Line (White with dynamic alert levels, never royal blue)
                            LinearProgressIndicator(
                                progress = { storage.usedPercentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = progressColor,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(usedPct * 100).toInt()}% Used",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap to manage & free up space",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // External SD Card / Secondary storage if present
                if (storageList.size > 1) {
                    val secondary = storageList[1]
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToFolder(File(secondary.path)) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SdCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(secondary.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("${secondary.formattedUsed} used of ${secondary.formattedTotal}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Browse", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // CATEGORIES HEADER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Categories",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 8 CLEAN CATEGORY GRID TILES (Adaptive for tablets/phones)
            item {
                val isTablet = LocalConfiguration.current.screenWidthDp >= 600
                val gridColumns = if (isTablet) 4 else 2
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val rows = categories.chunked(gridColumns)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { cat ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onCategoryClick(cat.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = cat.iconTint.copy(alpha = 0.12f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = cat.icon,
                                                    contentDescription = null,
                                                    tint = cat.iconTint,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = cat.title,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // QUICK ACCESS FOLDERS
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = {
                        val base = Environment.getExternalStorageDirectory()
                        if (base != null) onNavigateToFolder(base)
                    }) {
                        Text("Browse Storage", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickFolders) { folder ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { onNavigateToFolder(File(folder.path)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = folder.name,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = folder.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // CONTINUE WATCHING (if recent video available)
            if (recentVideos.isNotEmpty()) {
                val lastVideo = recentVideos.first()
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Continue Watching",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenFile(
                                    MediaItem(
                                        uri = android.net.Uri.parse(lastVideo.uri),
                                        name = lastVideo.title,
                                        path = lastVideo.path,
                                        mediaType = MediaType.VIDEO,
                                        durationMs = lastVideo.durationMs
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lastVideo.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatTime(lastVideo.positionMs)} / ${formatTime(lastVideo.durationMs)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // RECENTLY OPENED ARCHIVES
            if (recentArchives.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Recently Opened Archives",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    recentArchives.take(3).forEach { arch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val file = File(arch.path)
                                    onOpenFile(FileManagerHelper.fileToMediaItem(file))
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = Color(0xFFFBBC04),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = arch.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${arch.entryCount} items • ${arch.format}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About GMK Player", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("GMK Player v1.0", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Developer: GMK Studios", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Combines Google Files-inspired browsing, ZArchiver archive management, and VLC-style media playback with hardware acceleration, encrypted vault, and Wi-Fi file transfer.", fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Package: com.gmk.mediaplayer", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }
}
