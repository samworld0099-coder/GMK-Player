package com.example.cleanup

import android.content.Context
import android.os.Environment
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.StorageInfo
import com.example.filemanager.FileManagerHelper
import com.example.trash.TrashManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CleanupCategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconTint: Color,
    val items: List<MediaItem>
) {
    val totalBytes: Long get() = items.sumOf { it.size }
    val formattedSize: String get() = StorageInfo.formatBytes(totalBytes)
}

enum class CleanupSortOption {
    SIZE_DESC,
    DATE_DESC,
    NAME_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCleanupScreen(
    onOpenFile: (MediaItem) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isScanning by remember { mutableStateOf(true) }
    var storageList by remember { mutableStateOf<List<StorageInfo>>(emptyList()) }
    var appCacheBytes by remember { mutableLongStateOf(0L) }

    // 9 Categories
    var largeFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var unusedFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var downloadFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var imageFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var videoFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var audioFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var documentFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var apkFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var otherFiles by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    var duplicateCandidates by remember { mutableStateOf<List<List<MediaItem>>>(emptyList()) }

    // Selected category for drill-down inspection
    var activeCategory by remember { mutableStateOf<CleanupCategory?>(null) }

    // Global multi-selection for cleanup
    var selectedFilesToDelete by remember { mutableStateOf<Set<File>>(emptySet()) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var showMoveToTrashDialog by remember { mutableStateOf(false) }

    // Sorting state for detail view
    var sortOption by remember { mutableStateOf(CleanupSortOption.SIZE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    fun scanStorage() {
        scope.launch {
            isScanning = true
            storageList = FileManagerHelper.getStorageVolumes(context)

            withContext(Dispatchers.IO) {
                // 1. Calculate application cache
                val cacheDir = context.cacheDir
                val extCache = context.externalCacheDir
                val c1 = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                val c2 = extCache?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
                appCacheBytes = c1 + c2

                // 2. Discover media files from standard locations
                val scanDirs = listOfNotNull(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                )

                val allFound = mutableListOf<MediaItem>()
                val sizeMap = mutableMapOf<Long, MutableList<MediaItem>>()

                val sixtyDaysAgo = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)

                for (dir in scanDirs) {
                    if (dir.exists()) {
                        dir.walkTopDown().maxDepth(3).forEach { file ->
                            if (file.isFile && file.length() > 0) {
                                val item = FileManagerHelper.fileToMediaItem(file)
                                allFound.add(item)

                                if (item.size > 1024 * 1024) {
                                    sizeMap.getOrPut(item.size) { mutableListOf() }.add(item)
                                }
                            }
                        }
                    }
                }

                // Partition into the 9 categories
                // 1. Large files (> 50 MB)
                largeFiles = allFound.filter { it.size >= 50L * 1024 * 1024 }.sortedByDescending { it.size }

                // 2. Unused files (> 60 days untouched)
                unusedFiles = allFound.filter {
                    val f = File(it.path)
                    f.exists() && f.lastModified() < sixtyDaysAgo
                }.sortedByDescending { it.size }

                // 3. Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dPath = downloadsDir?.absolutePath ?: ""
                downloadFiles = allFound.filter { it.path.startsWith(dPath) }.sortedByDescending { it.size }

                // 4. Images
                imageFiles = allFound.filter { it.mediaType == MediaType.IMAGE }.sortedByDescending { it.size }

                // 5. Videos
                videoFiles = allFound.filter { it.mediaType == MediaType.VIDEO }.sortedByDescending { it.size }

                // 6. Audio
                audioFiles = allFound.filter { it.mediaType == MediaType.AUDIO }.sortedByDescending { it.size }

                // 7. Documents
                documentFiles = allFound.filter { it.mediaType == MediaType.DOCUMENT }.sortedByDescending { it.size }

                // 8. APK Files (.apk)
                apkFiles = allFound.filter { it.extension.equals("apk", ignoreCase = true) }.sortedByDescending { it.size }

                // 9. Other Files (Archives, miscellaneous)
                otherFiles = allFound.filter {
                    it.mediaType == MediaType.ARCHIVE ||
                    (it.mediaType == MediaType.OTHER && !it.extension.equals("apk", ignoreCase = true))
                }.sortedByDescending { it.size }

                // Duplicates: size match and name or size collision
                duplicateCandidates = sizeMap.values.filter { it.size > 1 }
            }
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        scanStorage()
    }

    BackHandler {
        if (activeCategory != null) {
            activeCategory = null
        } else {
            onBack()
        }
    }

    val totalSelectedBytes = remember(selectedFilesToDelete) {
        selectedFilesToDelete.sumOf { it.length() }
    }

    val estimatedCleanableBytes = remember(appCacheBytes, unusedFiles, apkFiles, duplicateCandidates) {
        val dupesExcess = duplicateCandidates.sumOf { group ->
            if (group.size > 1) group.drop(1).sumOf { it.size } else 0L
        }
        appCacheBytes + (unusedFiles.take(15).sumOf { it.size } / 2) + apkFiles.sumOf { it.size } + dupesExcess
    }

    val categoryList = remember(largeFiles, unusedFiles, downloadFiles, imageFiles, videoFiles, audioFiles, documentFiles, apkFiles, otherFiles) {
        listOf(
            CleanupCategory("large_files", "Large Files (>50MB)", Icons.Default.CleaningServices, Color(0xFFEA4335), largeFiles),
            CleanupCategory("unused_files", "Unused Files (>60d)", Icons.Default.Warning, Color(0xFFF29900), unusedFiles),
            CleanupCategory("downloads", "Downloads", Icons.Default.FileDownload, Color(0xFF129EAF), downloadFiles),
            CleanupCategory("images", "Images", Icons.Default.Image, Color(0xFF1A73E8), imageFiles),
            CleanupCategory("videos", "Videos", Icons.Default.Movie, Color(0xFFEA4335), videoFiles),
            CleanupCategory("audio", "Audio Tracks", Icons.Default.MusicNote, Color(0xFF34A853), audioFiles),
            CleanupCategory("documents", "Documents", Icons.Default.Description, Color(0xFF4285F4), documentFiles),
            CleanupCategory("apk_files", "APK Installers", Icons.Default.Android, Color(0xFF0F9D58), apkFiles),
            CleanupCategory("other_files", "Other & Archives", Icons.Default.FolderZip, Color(0xFFFBBC04), otherFiles)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activeCategory?.title ?: "Storage Management & Cleanup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeCategory != null) activeCategory = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeCategory != null) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Size (Largest first)") },
                                    onClick = {
                                        sortOption = CleanupSortOption.SIZE_DESC
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Newest first)") },
                                    onClick = {
                                        sortOption = CleanupSortOption.DATE_DESC
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A to Z)") },
                                    onClick = {
                                        sortOption = CleanupSortOption.NAME_ASC
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { scanStorage() }) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Rescan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Combined Multi-Selection Cleanup Action Banner
            if (selectedFilesToDelete.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected ${selectedFilesToDelete.size} files (${StorageInfo.formatBytes(totalSelectedBytes)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { selectedFilesToDelete = emptySet() }) {
                                Text("Clear Selection", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Option 1: Soft Delete to Bin (Recoverable)
                            OutlinedButton(
                                onClick = { showMoveToTrashDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Move to Bin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Option 2: Permanent Deletion
                            Button(
                                onClick = { showPermanentDeleteDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Forever", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(42.dp), color = Color(0xFF0D47A1))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Scanning storage & media categories...", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else if (activeCategory != null) {
            // ==========================================
            // DETAILED CATEGORY FILE LIST VIEW
            // ==========================================
            val currentCat = activeCategory!!
            val rawItems = currentCat.items
            val sortedItems = remember(rawItems, sortOption) {
                when (sortOption) {
                    CleanupSortOption.SIZE_DESC -> rawItems.sortedByDescending { it.size }
                    CleanupSortOption.DATE_DESC -> rawItems.sortedByDescending { File(it.path).lastModified() }
                    CleanupSortOption.NAME_ASC -> rawItems.sortedBy { it.name.lowercase(Locale.ROOT) }
                }
            }

            val categoryFiles = remember(sortedItems) { sortedItems.map { File(it.path) }.toSet() }
            val allCategorySelected = categoryFiles.isNotEmpty() && selectedFilesToDelete.containsAll(categoryFiles)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Header Card with Category Summary & Select All Toggle
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = currentCat.iconTint.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = currentCat.icon,
                                        contentDescription = null,
                                        tint = currentCat.iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(currentCat.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = "${currentCat.items.size} files • ${currentCat.formattedSize}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    selectedFilesToDelete = if (allCategorySelected) {
                                        selectedFilesToDelete - categoryFiles
                                    } else {
                                        selectedFilesToDelete + categoryFiles
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (allCategorySelected) "Deselect" else "Select All", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (sortedItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No files in this category", color = Color.Gray)
                        }
                    }
                } else {
                    items(sortedItems, key = { it.path }) { item ->
                        val file = File(item.path)
                        val isSelected = selectedFilesToDelete.contains(file)
                        val dateStr = remember(item.path) {
                            val lm = file.lastModified()
                            if (lm > 0) SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(lm)) else ""
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenFile(item) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedFilesToDelete = if (checked) selectedFilesToDelete + file else selectedFilesToDelete - file
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(item.formattedSize, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        if (dateStr.isNotEmpty()) {
                                            Text("• $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(item.path, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // MAIN STORAGE OVERVIEW & 9 CATEGORIES
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("storage_cleanup_container"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Storage Bar Overview (Royal Blue container with White Usage Line)
                val primary = storageList.firstOrNull()
                primary?.let { storage ->
                    item {
                        val usedPct = storage.usedPercentage
                        val progressColor = when {
                            usedPct >= 0.90f -> Color(0xFFF44336)
                            usedPct >= 0.80f -> Color(0xFFFF9800)
                            usedPct >= 0.70f -> Color(0xFFFFEB3B)
                            else -> Color.White
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.20f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SdStorage,
                                                contentDescription = null,
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
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${storage.formattedUsed} used of ${storage.formattedTotal} • ${storage.formattedFree} free",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.90f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                LinearProgressIndicator(
                                    progress = { storage.usedPercentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = progressColor,
                                    trackColor = Color.White.copy(alpha = 0.25f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(usedPct * 100).toInt()}% Consumed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Cleanable: ~${StorageInfo.formatBytes(estimatedCleanableBytes)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFEB3B)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. App Cache Quick Clean Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Junk & Temporary Cache", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "Cached video buffers, previews & logs: ${StorageInfo.formatBytes(appCacheBytes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val freed = appCacheBytes
                                        withContext(Dispatchers.IO) {
                                            context.cacheDir.deleteRecursively()
                                            context.externalCacheDir?.deleteRecursively()
                                            appCacheBytes = 0L
                                        }
                                        snackbarHostState.showSnackbar("Cleared ${StorageInfo.formatBytes(freed)} of temporary app cache")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                enabled = appCacheBytes > 0L
                            ) {
                                Text("Clean Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. Smart Cleanup Suggestions (Large files, old downloads, duplicate detection)
                item {
                    Text(
                        text = "Smart Cleanup Recommendations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (largeFiles.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeCategory = categoryList.firstOrNull { it.id == "large_files" }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFEA4335))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Free up space from Large Files", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${largeFiles.size} files larger than 50MB (${StorageInfo.formatBytes(largeFiles.sumOf { it.size })})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                            }
                        }
                    }
                }

                if (apkFiles.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeCategory = categoryList.firstOrNull { it.id == "apk_files" }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF0F9D58))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Uninstallable / Old APK Installers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${apkFiles.size} APK files taking ${StorageInfo.formatBytes(apkFiles.sumOf { it.size })}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("View", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                            }
                        }
                    }
                }

                // 4. 9 Comprehensive Categories
                item {
                    Text(
                        text = "Storage Categories (9)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(categoryList, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeCategory = cat },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = cat.iconTint.copy(alpha = 0.12f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = cat.iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    "${cat.items.size} items",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = cat.formattedSize,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 5. Safety Guarantee Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF0D47A1),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "GMK Safety Guarantee: No files are ever deleted without your explicit consent. You can soft-delete files to Recently Deleted (Bin) or remove them permanently.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // ACTION DIALOG: MOVE TO TRASH / BIN
    // ==========================================
    if (showMoveToTrashDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToTrashDialog = false },
            title = { Text("Move to Recently Deleted?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Move ${selectedFilesToDelete.size} selected file(s) (${StorageInfo.formatBytes(totalSelectedBytes)}) to the Recently Deleted Bin?\n\n" +
                    "Files will free up device storage and can be restored anytime from Recently Deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMoveToTrashDialog = false
                        scope.launch {
                            val freed = totalSelectedBytes
                            withContext(Dispatchers.IO) {
                                for (file in selectedFilesToDelete) {
                                    TrashManager.moveToTrash(context, file, isPrivate = false)
                                }
                            }
                            selectedFilesToDelete = emptySet()
                            scanStorage()
                            snackbarHostState.showSnackbar("Moved to Bin (Freed ${StorageInfo.formatBytes(freed)})")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
                ) {
                    Text("Move to Bin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveToTrashDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // ACTION DIALOG: PERMANENT DELETION
    // ==========================================
    if (showPermanentDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showPermanentDeleteDialog = false },
            title = { Text("Permanently Delete Files?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to PERMANENTLY delete ${selectedFilesToDelete.size} selected file(s)?\n\n" +
                    "Total space to be reclaimed: ${StorageInfo.formatBytes(totalSelectedBytes)}.\n" +
                    "This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermanentDeleteDialog = false
                        scope.launch {
                            val freed = totalSelectedBytes
                            withContext(Dispatchers.IO) {
                                for (file in selectedFilesToDelete) {
                                    file.delete()
                                }
                            }
                            selectedFilesToDelete = emptySet()
                            scanStorage()
                            snackbarHostState.showSnackbar("Permanently deleted ${StorageInfo.formatBytes(freed)}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermanentDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
