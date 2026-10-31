package com.example.ui.files

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.SelectAll
import com.example.trash.TrashManager
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.archive.ArchiveManager
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.filemanager.ConflictResolution
import com.example.filemanager.FileManagerHelper
import com.example.filemanager.SortField
import com.example.filemanager.SortOrder
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ClipboardState(
    val files: List<File>,
    val isCut: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    initialDirectory: File,
    onOpenFile: (MediaItem) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDir by remember { mutableStateOf(initialDirectory) }
    var fileItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItems by remember { mutableStateOf<Set<File>>(emptySet()) }
    var isGridView by remember { mutableStateOf(false) }
    var sortField by remember { mutableStateOf(SortField.NAME) }
    var sortOrder by remember { mutableStateOf(SortOrder.ASCENDING) }
    var showSortMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Dialog states
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialogFor by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPropertiesDialogFor by remember { mutableStateOf<MediaItem?>(null) }
    var showCreateZipDialog by remember { mutableStateOf(false) }
    var showMoveToDialog by remember { mutableStateOf(false) }
    var showCopyToDialog by remember { mutableStateOf(false) }
    var singleItemForMoveOrCopy by remember { mutableStateOf<File?>(null) }

    // Clipboard
    var clipboard by remember { mutableStateOf<ClipboardState?>(null) }

    fun refreshList() {
        scope.launch {
            fileItems = FileManagerHelper.listDirectory(
                directory = currentDir,
                sortField = sortField,
                sortOrder = sortOrder
            )
        }
    }

    LaunchedEffect(currentDir, sortField, sortOrder) {
        refreshList()
    }

    BackHandler {
        val parent = currentDir.parentFile
        if (parent != null && parent.canRead()) {
            currentDir = parent
        } else {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (selectedItems.isNotEmpty()) {
                        Text("${selectedItems.size} selected", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    } else {
                        Column {
                            Text(
                                text = currentDir.name.ifEmpty { "Storage" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${fileItems.size} items",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(onClick = { selectedItems = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect")
                        }
                    } else {
                        IconButton(onClick = {
                            val parent = currentDir.parentFile
                            if (parent != null && parent.canRead()) {
                                currentDir = parent
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        // Batch actions
                        IconButton(onClick = {
                            val allFiles = fileItems.map { File(it.path) }.toSet()
                            selectedItems = if (selectedItems.size == allFiles.size) emptySet() else allFiles
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            singleItemForMoveOrCopy = null
                            showMoveToDialog = true
                        }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move To")
                        }
                        IconButton(onClick = {
                            singleItemForMoveOrCopy = null
                            showCopyToDialog = true
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy To")
                        }
                        IconButton(onClick = { showCreateZipDialog = true }) {
                            Icon(Icons.Default.FolderZip, contentDescription = "Create Archive")
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List"
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)") },
                                    onClick = {
                                        sortField = SortField.NAME
                                        sortOrder = SortOrder.ASCENDING
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date (Newest first)") },
                                    onClick = {
                                        sortField = SortField.DATE
                                        sortOrder = SortOrder.DESCENDING
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Size (Largest first)") },
                                    onClick = {
                                        sortField = SortField.SIZE
                                        sortOrder = SortOrder.DESCENDING
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Type") },
                                    onClick = {
                                        sortField = SortField.TYPE
                                        sortOrder = SortOrder.ASCENDING
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier.testTag("create_folder_fab")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                }
                FloatingActionButton(
                    onClick = { showNewFileDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "New File")
                }
            }
        }
    ) { innerPadding ->
        val isTablet = LocalConfiguration.current.screenWidthDp >= 600
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTablet) {
                TabletStorageSidebar(
                    currentDir = currentDir,
                    onNavigate = { dir -> currentDir = dir },
                    modifier = Modifier.width(260.dp)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
            // Search Input
            AnimatedVisibility(visible = isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or extension...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    singleLine = true
                )
            }

            // Path Breadcrumb Bar
            BreadcrumbBar(
                currentDir = currentDir,
                onNavigate = { dir -> currentDir = dir }
            )

            // Clipboard Paste Bar
            clipboard?.let { clip ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${clip.files.size} items ready to ${if (clip.isCut) "move" else "copy"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    for (src in clip.files) {
                                        if (clip.isCut) {
                                            FileManagerHelper.move(src, currentDir, ConflictResolution.RENAME)
                                        } else {
                                            FileManagerHelper.copy(src, currentDir, ConflictResolution.RENAME)
                                        }
                                    }
                                    clipboard = null
                                    refreshList()
                                    snackbarHostState.showSnackbar("Pasted successfully")
                                }
                            }
                        ) {
                            Text("Paste Here", fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { clipboard = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }

            // File items view
            val filtered = fileItems.filter {
                searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No files match \"$searchQuery\"" else "Folder is empty",
                        color = Color.Gray
                    )
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filtered, key = { it.path }) { item ->
                        val file = File(item.path)
                        val isSelected = selectedItems.contains(file)
                        FileGridItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (selectedItems.isNotEmpty()) {
                                    selectedItems = if (isSelected) selectedItems - file else selectedItems + file
                                } else {
                                    if (item.isDirectory) currentDir = file else onOpenFile(item)
                                }
                            },
                            onLongClick = {
                                selectedItems = if (isSelected) selectedItems - file else selectedItems + file
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("file_browser_list")
                ) {
                    items(filtered, key = { it.path }) { item ->
                        val file = File(item.path)
                        val isSelected = selectedItems.contains(file)
                        var showItemMenu by remember { mutableStateOf(false) }

                        FileListItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (selectedItems.isNotEmpty()) {
                                    selectedItems = if (isSelected) selectedItems - file else selectedItems + file
                                } else {
                                    if (item.isDirectory) currentDir = file else onOpenFile(item)
                                }
                            },
                            onLongClick = {
                                selectedItems = if (isSelected) selectedItems - file else selectedItems + file
                            },
                            onMoreClick = { showItemMenu = true }
                        )

                        DropdownMenu(
                            expanded = showItemMenu,
                            onDismissRequest = { showItemMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open") },
                                onClick = {
                                    showItemMenu = false
                                    if (item.isDirectory) currentDir = file else onOpenFile(item)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add / Remove Favorite") },
                                onClick = {
                                    showItemMenu = false
                                    onToggleFavorite(item)
                                    scope.launch { snackbarHostState.showSnackbar("Favorites updated") }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showItemMenu = false
                                    showRenameDialogFor = file
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move To...") },
                                onClick = {
                                    showItemMenu = false
                                    singleItemForMoveOrCopy = file
                                    showMoveToDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy To...") },
                                onClick = {
                                    showItemMenu = false
                                    singleItemForMoveOrCopy = file
                                    showCopyToDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Details & Properties") },
                                onClick = {
                                    showItemMenu = false
                                    showPropertiesDialogFor = item
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showItemMenu = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = item.mimeType ?: "*/*"
                                        putExtra(Intent.EXTRA_STREAM, item.uri)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share ${item.name}"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete (Move to Trash)", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showItemMenu = false
                                    scope.launch {
                                        val res = TrashManager.moveToTrash(context, file, isPrivate = false)
                                        if (res.isSuccess) {
                                            refreshList()
                                            snackbarHostState.showSnackbar("Moved to Recently Deleted")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed to delete")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewFolderDialog = false
                        scope.launch {
                            val res = FileManagerHelper.createFolder(currentDir, folderName)
                            if (res.isSuccess) {
                                refreshList()
                                snackbarHostState.showSnackbar("Folder created")
                            } else {
                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error")
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // New Text File Dialog
    if (showNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        var fileContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New Text File", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Filename (.txt)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        label = { Text("Initial Content (Optional)") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewFileDialog = false
                        scope.launch {
                            val res = FileManagerHelper.createTextFile(currentDir, fileName, fileContent)
                            if (res.isSuccess) {
                                refreshList()
                                snackbarHostState.showSnackbar("File created")
                            } else {
                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error")
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    showRenameDialogFor?.let { file ->
        var newName by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialogFor = null },
            title = { Text("Rename", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialogFor = null
                        scope.launch {
                            val res = FileManagerHelper.rename(file, newName)
                            if (res.isSuccess) {
                                refreshList()
                                snackbarHostState.showSnackbar("Renamed")
                            } else {
                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Error")
                            }
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogFor = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Files?", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete ${selectedItems.size} selected item(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        scope.launch {
                            for (f in selectedItems) {
                                FileManagerHelper.deleteRecursively(f)
                            }
                            selectedItems = emptySet()
                            refreshList()
                            snackbarHostState.showSnackbar("Deleted items")
                        }
                    }
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Properties Dialog
    showPropertiesDialogFor?.let { prop ->
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { showPropertiesDialogFor = null },
            title = { Text("File Properties", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Name: ${prop.name}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Path: ${prop.path}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Size: ${prop.formattedSize}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Type: ${prop.mediaType.name} (${prop.extension.ifEmpty { "Folder" }})")
                    prop.mimeType?.let { Text("MIME: $it") }
                    prop.resolution?.let { Text("Resolution: $it") }
                    if (prop.durationMs > 0) Text("Duration: ${prop.formattedDuration}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Modified: ${dateFormat.format(Date(prop.lastModified))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialogFor = null }) { Text("Close") }
            }
        )
    }

    // Create Archive Dialog
    if (showCreateZipDialog) {
        var zipName by remember { mutableStateOf("Archive_${System.currentTimeMillis() / 1000}.zip") }
        AlertDialog(
            onDismissRequest = { showCreateZipDialog = false },
            title = { Text("Create ZIP Archive", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("Archive Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCreateZipDialog = false
                        val cleanZip = if (zipName.endsWith(".zip")) zipName else "$zipName.zip"
                        val outZip = File(currentDir, cleanZip)
                        scope.launch {
                            val res = ArchiveManager.createZipArchive(
                                sources = selectedItems.toList(),
                                outputZipFile = outZip,
                                onProgress = { _, _ -> }
                            )
                            if (res.isSuccess) {
                                selectedItems = emptySet()
                                refreshList()
                                snackbarHostState.showSnackbar("Archive created: ${outZip.name}")
                            } else {
                                snackbarHostState.showSnackbar("Failed to create archive: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateZipDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showMoveToDialog) {
        val targets = singleItemForMoveOrCopy?.let { listOf(it) } ?: selectedItems.toList()
        DestinationFolderPickerDialog(
            title = "Move ${targets.size} item(s) To",
            actionButtonText = "Move Here",
            onConfirm = { destDir, conflict ->
                showMoveToDialog = false
                scope.launch {
                    var successCount = 0
                    for (file in targets) {
                        val res = FileManagerHelper.move(file, destDir, conflict)
                        if (res.isSuccess) {
                            successCount++
                            res.getOrNull()?.let { movedFile ->
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(movedFile.absolutePath), null, null)
                            }
                        }
                    }
                    selectedItems = emptySet()
                    singleItemForMoveOrCopy = null
                    refreshList()
                    snackbarHostState.showSnackbar("Moved $successCount item(s) to ${destDir.name}")
                }
            },
            onDismiss = {
                showMoveToDialog = false
                singleItemForMoveOrCopy = null
            }
        )
    }

    if (showCopyToDialog) {
        val targets = singleItemForMoveOrCopy?.let { listOf(it) } ?: selectedItems.toList()
        DestinationFolderPickerDialog(
            title = "Copy ${targets.size} item(s) To",
            actionButtonText = "Copy Here",
            onConfirm = { destDir, conflict ->
                showCopyToDialog = false
                scope.launch {
                    var successCount = 0
                    for (file in targets) {
                        val res = FileManagerHelper.copy(file, destDir, conflict)
                        if (res.isSuccess) {
                            successCount++
                            res.getOrNull()?.let { copiedFile ->
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(copiedFile.absolutePath), null, null)
                            }
                        }
                    }
                    selectedItems = emptySet()
                    singleItemForMoveOrCopy = null
                    refreshList()
                    snackbarHostState.showSnackbar("Copied $successCount item(s) to ${destDir.name}")
                }
            },
            onDismiss = {
                showCopyToDialog = false
                singleItemForMoveOrCopy = null
            }
        )
    }
}

@Composable
fun BreadcrumbBar(
    currentDir: File,
    onNavigate: (File) -> Unit
) {
    val crumbs = mutableListOf<File>()
    var cur: File? = currentDir
    while (cur != null) {
        crumbs.add(0, cur)
        cur = cur.parentFile
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(crumbs) { dir ->
            val isCurrent = dir == currentDir
            Text(
                text = if (dir.name.isEmpty()) "Root" else dir.name,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onNavigate(dir) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                fontSize = 13.sp
            )
            if (dir != crumbs.last()) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FileListItem(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getMediaIcon(item),
            contentDescription = null,
            tint = getMediaTint(item),
            modifier = Modifier.size(36.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (item.isDirectory) item.formattedSize
                else "${item.formattedSize} • ${item.extension.uppercase()}${if (item.formattedDuration.isNotEmpty()) " • ${item.formattedDuration}" else ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun FileGridItem(
    item: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .padding(6.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getMediaIcon(item),
                contentDescription = null,
                tint = getMediaTint(item),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.formattedSize,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getMediaIcon(item: MediaItem) = when {
    item.isDirectory -> Icons.Default.Folder
    item.isArchive -> Icons.Default.FolderZip
    item.mediaType == MediaType.VIDEO -> Icons.Default.Movie
    item.mediaType == MediaType.AUDIO -> Icons.Default.MusicNote
    item.mediaType == MediaType.IMAGE -> Icons.Default.Image
    item.mediaType == MediaType.DOCUMENT -> Icons.Default.Description
    else -> Icons.Default.InsertDriveFile
}

fun getMediaTint(item: MediaItem) = when {
    item.isDirectory -> Color(0xFFFFA000)
    item.isArchive -> Color(0xFFFFAB00)
    item.mediaType == MediaType.VIDEO -> Color(0xFFFF5252)
    item.mediaType == MediaType.AUDIO -> Color(0xFF448AFF)
    item.mediaType == MediaType.IMAGE -> Color(0xFF4CAF50)
    item.mediaType == MediaType.DOCUMENT -> Color(0xFF00B0FF)
    else -> Color.Gray
}

@Composable
fun TabletStorageSidebar(
    currentDir: File,
    onNavigate: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val shortcuts = remember {
        listOf(
            "Internal Storage" to Environment.getExternalStorageDirectory(),
            "Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Pictures" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "DCIM (Camera)" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Movies" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Documents" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        ).filter { it.second != null && it.second.exists() }
    }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = "Locations",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
            shortcuts.forEach { (title, dir) ->
                val isSelected = currentDir.absolutePath == dir.absolutePath || currentDir.absolutePath.startsWith(dir.absolutePath + "/")
                val icon = when {
                    title.contains("Download", true) -> Icons.Default.FileDownload
                    title.contains("Picture", true) -> Icons.Default.Image
                    title.contains("DCIM", true) -> Icons.Default.Movie
                    title.contains("Movie", true) -> Icons.Default.Movie
                    title.contains("Music", true) -> Icons.Default.MusicNote
                    title.contains("Doc", true) -> Icons.Default.Description
                    else -> Icons.Default.SdStorage
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF0D47A1).copy(alpha = 0.15f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(dir) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

