package com.example.ui.archive

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.archive.ArchiveDirectoryNode
import com.example.archive.ArchiveManager
import com.example.archive.ArchiveTreeHelper
import com.example.data.model.ArchiveEntryItem
import com.example.data.model.ArchiveExtractionProgress
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveBrowserScreen(
    archiveFile: File,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var allEntries by remember { mutableStateOf<List<ArchiveEntryItem>>(emptyList()) }
    var currentVirtualPath by remember { mutableStateOf("") }
    var selectedEntries by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Extraction progress state
    var extractionProgress by remember { mutableStateOf<ArchiveExtractionProgress?>(null) }
    var isCancelled by remember { mutableStateOf(false) }
    var showExtractConfirmDialog by remember { mutableStateOf(false) }
    var extractOnlySelected by remember { mutableStateOf(false) }

    // Load archive entries
    LaunchedEffect(archiveFile) {
        isLoading = true
        errorMessage = null
        val result = ArchiveManager.listEntries(archiveFile)
        isLoading = false
        if (result.isSuccess) {
            allEntries = result.getOrNull() ?: emptyList()
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to open archive"
        }
    }

    // Virtual folder contents
    val currentNode: ArchiveDirectoryNode = remember(allEntries, currentVirtualPath) {
        ArchiveTreeHelper.getEntriesInDirectory(allEntries, currentVirtualPath)
    }

    val breadcrumbs = remember(currentVirtualPath) {
        ArchiveTreeHelper.getBreadcrumbs(currentVirtualPath)
    }

    BackHandler {
        if (currentVirtualPath.isNotEmpty()) {
            val slashIdx = currentVirtualPath.lastIndexOf('/')
            currentVirtualPath = if (slashIdx >= 0) currentVirtualPath.substring(0, slashIdx) else ""
        } else {
            onBack()
        }
    }

    fun startExtraction(onlySelected: Boolean) {
        val destDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GMK_Extracted/${archiveFile.nameWithoutExtension}"
        )
        isCancelled = false
        scope.launch {
            val selected = if (onlySelected) selectedEntries else null
            val result = ArchiveManager.extract(
                archiveFile = archiveFile,
                destinationDir = destDir,
                selectedPaths = selected,
                onProgress = { progress -> extractionProgress = progress },
                isCancelled = { isCancelled }
            )
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("Extracted ${result.getOrNull()} files to ${destDir.name}")
                selectedEntries = emptySet()
            } else {
                snackbarHostState.showSnackbar("Extraction failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = archiveFile.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allEntries.size} entries inside",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentVirtualPath.isNotEmpty()) {
                            val slashIdx = currentVirtualPath.lastIndexOf('/')
                            currentVirtualPath = if (slashIdx >= 0) currentVirtualPath.substring(0, slashIdx) else ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Search archive")
                    }
                    Button(
                        onClick = {
                            extractOnlySelected = selectedEntries.isNotEmpty()
                            showExtractConfirmDialog = true
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedEntries.isNotEmpty()) "Extract (${selectedEntries.size})" else "Extract All")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search inside archive...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            // Breadcrumbs Navigation
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(breadcrumbs) { (name, path) ->
                    Text(
                        text = name,
                        fontWeight = if (path == currentVirtualPath) FontWeight.Bold else FontWeight.Normal,
                        color = if (path == currentVirtualPath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { currentVirtualPath = path }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        fontSize = 13.sp
                    )
                    if (path != breadcrumbs.last().second) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // Content Area
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reading archive entries...")
                    }
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Unable to inspect archive",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "Unknown archive error",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                val filteredFolders = currentNode.subfolders.filter {
                    searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
                }
                val filteredFiles = currentNode.files.filter {
                    searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
                }

                if (filteredFolders.isEmpty() && filteredFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This folder is empty", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().testTag("archive_entries_list")) {
                        // Subfolders
                        items(filteredFolders) { folder ->
                            val isSelected = selectedEntries.contains(folder.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentVirtualPath = folder.path }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedEntries = if (checked) selectedEntries + folder.path
                                        else selectedEntries - folder.path
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = folder.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        // Files
                        items(filteredFiles) { file ->
                            val isSelected = selectedEntries.contains(file.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEntries = if (isSelected) selectedEntries - file.path
                                        else selectedEntries + file.path
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedEntries = if (checked) selectedEntries + file.path
                                        else selectedEntries - file.path
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = "File",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${file.formattedSize} • ${file.method ?: "Stored"} • Ratio: ${file.compressionRatio}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    // Extraction Confirm Dialog
    if (showExtractConfirmDialog) {
        val count = if (extractOnlySelected) selectedEntries.size else allEntries.size
        AlertDialog(
            onDismissRequest = { showExtractConfirmDialog = false },
            title = { Text("Extract Archive", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Extract $count entries into Downloads/GMK_Extracted/${archiveFile.nameWithoutExtension}?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Path traversal safeguards active: all destination files validated.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExtractConfirmDialog = false
                        startExtraction(extractOnlySelected)
                    }
                ) {
                    Text("Extract")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtractConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Active Extraction Progress Modal Dialog
    extractionProgress?.let { prog ->
        if (!prog.isFinished) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Extracting Archive...", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { prog.progressFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = prog.currentFileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Extracted ${prog.entriesProcessed} / ${prog.totalEntries} entries (${ArchiveManager.detectFormat(archiveFile)})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        isCancelled = true
                        extractionProgress = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
