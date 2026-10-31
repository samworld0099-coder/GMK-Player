package com.example.ui.files

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filemanager.ConflictResolution
import com.example.filemanager.FileManagerHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DestinationFolderPickerDialog(
    title: String,
    actionButtonText: String,
    initialDir: File = Environment.getExternalStorageDirectory(),
    onConfirm: (destinationDir: File, conflict: ConflictResolution) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentDir by remember { mutableStateOf(initialDir) }
    var subdirs by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedConflict by remember { mutableStateOf(ConflictResolution.RENAME) }
    var showCreateSubfolder by remember { mutableStateOf(false) }

    fun refreshSubdirs() {
        subdirs = currentDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    LaunchedEffect(currentDir) {
        refreshSubdirs()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Current path breadcrumb navigation
                val crumbs = mutableListOf<File>()
                var cur: File? = currentDir
                while (cur != null) {
                    crumbs.add(0, cur)
                    cur = cur.parentFile
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(crumbs) { dir ->
                        val isLast = dir == currentDir
                        Text(
                            text = if (dir.name.isEmpty()) "Root" else dir.name,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLast) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier
                                .clickable { currentDir = dir }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 12.sp
                        )
                        if (!isLast) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Destination Folder:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { showCreateSubfolder = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(20.dp))
                    }
                }

                // Subfolders list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (subdirs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No subfolders here", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        items(subdirs) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { currentDir = dir }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dir.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("If file already exists:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedConflict == ConflictResolution.RENAME,
                        onClick = { selectedConflict = ConflictResolution.RENAME }
                    )
                    Text("Keep Both", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = selectedConflict == ConflictResolution.REPLACE,
                        onClick = { selectedConflict = ConflictResolution.REPLACE }
                    )
                    Text("Replace", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = selectedConflict == ConflictResolution.SKIP,
                        onClick = { selectedConflict = ConflictResolution.SKIP }
                    )
                    Text("Skip", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(currentDir, selectedConflict)
            }) {
                Text(actionButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCreateSubfolder) {
        var folderNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateSubfolder = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clean = folderNameInput.trim()
                    if (clean.isNotEmpty()) {
                        scope.launch {
                            FileManagerHelper.createFolder(currentDir, clean)
                            refreshSubdirs()
                            showCreateSubfolder = false
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSubfolder = false }) { Text("Cancel") }
            }
        )
    }
}
