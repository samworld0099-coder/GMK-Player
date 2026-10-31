package com.example.ui.videos

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.VideoOrigin
import com.example.filemanager.ConflictResolution
import com.example.filemanager.FileManagerHelper
import com.example.filemanager.MediaStoreHelper
import com.example.filemanager.VideoThumbnailLoader
import com.example.trash.TrashManager
import com.example.ui.files.DestinationFolderPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VideoSortCriteria(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    SIZE_DESC("Size (Largest First)"),
    SIZE_ASC("Size (Smallest First)"),
    DURATION_DESC("Duration (Longest First)"),
    DURATION_ASC("Duration (Shortest First)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    videos: List<MediaItem>,
    onVideoClick: (MediaItem) -> Unit,
    onRefreshVideos: () -> Unit = {},
    onSendVideos: (List<File>) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedOriginFilter by remember { mutableStateOf(VideoOrigin.ALL) }
    var currentSort by remember { mutableStateOf(VideoSortCriteria.DATE_DESC) }

    // Multi-selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<Uri>() }

    // Dialogs state
    var showMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showManageFoldersDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingInProgress by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedUris.clear()
    }

    // Filter & Sort video list
    val filteredVideos = remember(videos, searchQuery, selectedOriginFilter, currentSort) {
        var result = videos

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            result = result.filter { it.name.lowercase(Locale.ROOT).contains(q) }
        }

        if (selectedOriginFilter != VideoOrigin.ALL) {
            result = result.filter { it.origin == selectedOriginFilter }
        }

        when (currentSort) {
            VideoSortCriteria.NAME_ASC -> result.sortedBy { it.name.lowercase(Locale.ROOT) }
            VideoSortCriteria.NAME_DESC -> result.sortedByDescending { it.name.lowercase(Locale.ROOT) }
            VideoSortCriteria.DATE_DESC -> result.sortedByDescending { it.lastModified }
            VideoSortCriteria.DATE_ASC -> result.sortedBy { it.lastModified }
            VideoSortCriteria.SIZE_DESC -> result.sortedByDescending { it.size }
            VideoSortCriteria.SIZE_ASC -> result.sortedBy { it.size }
            VideoSortCriteria.DURATION_DESC -> result.sortedByDescending { it.durationMs }
            VideoSortCriteria.DURATION_ASC -> result.sortedBy { it.durationMs }
        }
    }

    val selectedVideoItems = remember(selectedUris, videos) {
        videos.filter { selectedUris.contains(it.uri) }
    }

    fun toggleSelection(video: MediaItem) {
        if (selectedUris.contains(video.uri)) {
            selectedUris.remove(video.uri)
            if (selectedUris.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedUris.add(video.uri)
            isSelectionMode = true
        }
    }

    fun selectAll() {
        isSelectionMode = true
        selectedUris.clear()
        selectedUris.addAll(filteredVideos.map { it.uri })
    }

    fun deselectAll() {
        selectedUris.clear()
        isSelectionMode = false
    }

    fun shareSelected() {
        if (selectedVideoItems.isEmpty()) return
        val uris = ArrayList(selectedVideoItems.map { it.uri })
        val intent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
            type = "video/*"
            if (uris.size > 1) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            } else {
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Videos via"))
    }

    fun sendSelectedViaTransfer() {
        val files = selectedVideoItems.mapNotNull {
            if (it.path.isNotEmpty()) File(it.path) else null
        }
        if (files.isNotEmpty()) {
            onSendVideos(files)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Cannot resolve local file paths for transfer.")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                // Contextual Action Toolbar
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { deselectAll() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close selection")
                        }
                        Text(
                            text = "${selectedUris.size} Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Select All / Deselect All
                        IconButton(onClick = {
                            if (selectedUris.size == filteredVideos.size) deselectAll() else selectAll()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }

                        // Share
                        IconButton(onClick = { shareSelected() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }

                        // Move
                        IconButton(onClick = { showMoveDialog = true }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move")
                        }

                        // Bin / Delete
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }

                        // Send via Transfer
                        IconButton(onClick = { sendSelectedViaTransfer() }) {
                            Icon(Icons.Default.Send, contentDescription = "Send via GMK", tint = Color(0xFF1E88E5))
                        }
                    }
                }
            } else {
                // Standard Videos Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Text("Videos", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text(
                                "${videos.size} videos found",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        // View mode toggle
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Switch to List" else "Switch to Grid"
                            )
                        }

                        // Sort
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }

                        // Three-Dot Menu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.testTag("videos_overflow_menu_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Video options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select Videos") },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        isSelectionMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Select All") },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        selectAll()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort By") },
                                    leadingIcon = { Icon(Icons.Default.Sort, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showSortDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Refresh Library") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onRefreshVideos()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Media library refreshed")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage Folders") },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showManageFoldersDialog = true
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenSettings()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search videos by title...", fontSize = 14.sp) },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Origin / Source Filter Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val originTabs = listOf(
                    VideoOrigin.ALL,
                    VideoOrigin.WHATSAPP,
                    VideoOrigin.GMK_BROWSER,
                    VideoOrigin.CAMERA,
                    VideoOrigin.DOWNLOADS,
                    VideoOrigin.TELEGRAM,
                    VideoOrigin.BLUETOOTH,
                    VideoOrigin.RECEIVED,
                    VideoOrigin.OTHER
                )
                items(originTabs) { origin ->
                    val count = if (origin == VideoOrigin.ALL) videos.size else videos.count { it.origin == origin }
                    FilterChip(
                        selected = selectedOriginFilter == origin,
                        onClick = { selectedOriginFilter = origin },
                        label = {
                            Text("${origin.displayName} ($count)", fontSize = 12.sp)
                        },
                        leadingIcon = if (selectedOriginFilter == origin) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Main Video Content or Empty State
            if (filteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedOriginFilter != VideoOrigin.ALL) "No matching videos" else "No videos found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search terms"
                            else "GMK Player automatically scans Movies, Downloads, DCIM, WhatsApp, Telegram, Bluetooth, and custom folders.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { onRefreshVideos() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rescan Folders")
                            }
                            OutlinedButton(onClick = { showManageFoldersDialog = true }) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage Folders")
                            }
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(165.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("videos_grid")
                    ) {
                        items(filteredVideos, key = { it.uri.toString() }) { video ->
                            val isSelected = selectedUris.contains(video.uri)
                            VideoGridCard(
                                video = video,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) toggleSelection(video)
                                    else onVideoClick(video)
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedUris.add(video.uri)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("videos_list")
                    ) {
                        items(filteredVideos, key = { it.uri.toString() }) { video ->
                            val isSelected = selectedUris.contains(video.uri)
                            VideoListRow(
                                video = video,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) toggleSelection(video)
                                    else onVideoClick(video)
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedUris.add(video.uri)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Videos By", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    VideoSortCriteria.values().forEach { criteria ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentSort = criteria
                                    showSortDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSort == criteria,
                                onClick = {
                                    currentSort = criteria
                                    showSortDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(criteria.label, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Close") }
            }
        )
    }

    // Manage Folders Dialog
    if (showManageFoldersDialog) {
        var customFolders by remember { mutableStateOf(MediaStoreHelper.getCustomFolders(context)) }
        var newFolderPathInput by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showManageFoldersDialog = false
                onRefreshVideos()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanned Folders", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "GMK Player automatically discovers videos in standard storage locations and any custom folders below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Automatic Media Sources:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Movies & Downloads", fontSize = 11.sp)
                            Text("• DCIM & Camera", fontSize = 11.sp)
                            Text("• WhatsApp Video (Public & Media)", fontSize = 11.sp)
                            Text("• Telegram Video", fontSize = 11.sp)
                            Text("• Bluetooth & Shared Transfers", fontSize = 11.sp)
                            Text("• GMK Browser Downloads & Received", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Custom User Folders (${customFolders.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (customFolders.isEmpty()) {
                        Text("No custom folders added yet.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        LazyColumn(modifier = Modifier.height(100.dp)) {
                            items(customFolders) { path ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        path,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            MediaStoreHelper.removeCustomFolder(context, path)
                                            customFolders = MediaStoreHelper.getCustomFolders(context)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Add Custom Directory Path:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newFolderPathInput,
                        onValueChange = {
                            newFolderPathInput = it
                            inputError = null
                        },
                        placeholder = { Text("/storage/emulated/0/YourFolder", fontSize = 12.sp) },
                        singleLine = true,
                        isError = inputError != null,
                        supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            val targetPath = newFolderPathInput.trim()
                            if (targetPath.isEmpty()) {
                                inputError = "Please enter a valid directory path"
                                return@Button
                            }
                            val dir = File(targetPath)
                            if (!dir.exists() || !dir.isDirectory) {
                                inputError = "Directory does not exist or is inaccessible"
                                return@Button
                            }
                            MediaStoreHelper.addCustomFolder(context, dir.absolutePath)
                            customFolders = MediaStoreHelper.getCustomFolders(context)
                            newFolderPathInput = ""
                            inputError = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Folder", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showManageFoldersDialog = false
                    onRefreshVideos()
                }) {
                    Text("Done & Rescan")
                }
            }
        )
    }

    // Move Dialog
    if (showMoveDialog) {
        DestinationFolderPickerDialog(
            title = "Move ${selectedVideoItems.size} Videos",
            actionButtonText = "Move Here",
            initialDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            onConfirm = { destinationDir, conflict ->
                showMoveDialog = false
                scope.launch {
                    var successCount = 0
                    for (item in selectedVideoItems) {
                        if (item.path.isNotEmpty()) {
                            val src = File(item.path)
                            if (src.exists()) {
                                val res = FileManagerHelper.move(src, destinationDir, conflict)
                                if (res.isSuccess) successCount++
                            }
                        }
                    }
                    deselectAll()
                    onRefreshVideos()
                    snackbarHostState.showSnackbar("Moved $successCount of ${selectedVideoItems.size} videos.")
                }
            },
            onDismiss = { showMoveDialog = false }
        )
    }

    // Delete Confirmation Dialog (Soft Trash vs Permanent)
    if (showDeleteConfirmDialog) {
        var deleteMode by remember { mutableStateOf("trash") } // "trash" or "permanent"
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = if (deleteMode == "trash") Icons.Default.RestoreFromTrash else Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete ${selectedVideoItems.size} Videos?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Choose deletion method for selected video files:",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteMode = "trash" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deleteMode == "trash",
                            onClick = { deleteMode = "trash" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Move to Trash (Soft Delete)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Files can be safely restored anytime from Recently Deleted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteMode = "permanent" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deleteMode == "permanent",
                            onClick = { deleteMode = "permanent" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Permanent Delete", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            Text("Irreversible. Files will be deleted directly from device storage.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        isDeletingInProgress = true
                        scope.launch {
                            var done = 0
                            for (item in selectedVideoItems) {
                                if (item.path.isNotEmpty()) {
                                    val f = File(item.path)
                                    if (f.exists()) {
                                        if (deleteMode == "trash") {
                                            TrashManager.moveToTrash(context, f, isPrivate = false)
                                            done++
                                        } else {
                                            if (f.delete()) done++
                                        }
                                    }
                                }
                            }
                            isDeletingInProgress = false
                            deselectAll()
                            onRefreshVideos()
                            snackbarHostState.showSnackbar(
                                if (deleteMode == "trash") "Moved $done videos to Recently Deleted."
                                else "Permanently deleted $done videos."
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (deleteMode == "trash") "Move to Trash" else "Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun VideoGridCard(
    video: MediaItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(video.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.uri) {
        thumbnailBitmap = VideoThumbnailLoader.loadThumbnail(context, video, width = 320, height = 220)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = video.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(46.dp)
                    )
                }

                // Origin / Source badge (Top-End)
                Surface(
                    color = getOriginBadgeColor(video.origin),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = video.origin.displayName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Resolution badge (Top-Start)
                video.resolution?.let { res ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = res,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Duration badge (Bottom-End)
                if (video.formattedDuration.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Selection Checkbox overlay
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent)
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() }
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${video.formattedSize} • ${video.extension.uppercase()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDateHelper(video.lastModified),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListRow(
    video: MediaItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(video.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.uri) {
        thumbnailBitmap = VideoThumbnailLoader.loadThumbnail(context, video, width = 240, height = 160)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 66.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2638)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = video.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (video.formattedDuration.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = video.formattedDuration,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = getOriginBadgeColor(video.origin),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = video.origin.displayName,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    video.resolution?.let { res ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(res, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${video.formattedSize} • ${video.extension.uppercase()} • ${formatDateHelper(video.lastModified)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getOriginBadgeColor(origin: VideoOrigin): Color {
    return when (origin) {
        VideoOrigin.WHATSAPP -> Color(0xFF25D366)   // WhatsApp Green
        VideoOrigin.GMK_BROWSER -> Color(0xFF1E88E5) // Royal Blue
        VideoOrigin.CAMERA -> Color(0xFFFFA000)      // Amber Camera
        VideoOrigin.DOWNLOADS -> Color(0xFF8E24AA)   // Deep Purple
        VideoOrigin.TELEGRAM -> Color(0xFF0088CC)    // Telegram Blue
        VideoOrigin.BLUETOOTH -> Color(0xFF3949AB)   // Indigo
        VideoOrigin.RECEIVED -> Color(0xFF00897B)    // Teal Received
        VideoOrigin.OTHER, VideoOrigin.ALL -> Color(0xFF546E7A) // Slate
    }
}

fun formatDateHelper(timestampMs: Long): String {
    if (timestampMs <= 0) return ""
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}
