package com.example.ui.trash

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trash.TrashItem
import com.example.trash.TrashManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var trashItems by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    fun refreshTrash() {
        scope.launch {
            trashItems = TrashManager.listTrash(context, isPrivate = false)
        }
    }

    LaunchedEffect(Unit) {
        refreshTrash()
    }

    BackHandler { onBack() }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trashItems.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("recently_deleted_screen")
        ) {
            // Notice banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Items deleted in GMK Media Player are kept here and can be restored. Files deleted outside the app are not recoverable.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trashItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(54.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Recently Deleted is empty", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Items deleted within the app will stay here for recovery", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(trashItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.formattedSize} • Deleted ${dateFormat.format(Date(item.deletedTimestamp))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Orig: ${item.originalPath}",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Restore button
                            IconButton(onClick = {
                                scope.launch {
                                    val res = TrashManager.restoreItem(context, item, isPrivate = false)
                                    if (res.isSuccess) {
                                        refreshTrash()
                                        snackbarHostState.showSnackbar("Restored ${item.fileName}")
                                    } else {
                                        snackbarHostState.showSnackbar("Restore failed: ${res.exceptionOrNull()?.message}")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore File", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Permanent delete button
                            IconButton(onClick = {
                                scope.launch {
                                    TrashManager.permanentlyDelete(context, item, isPrivate = false)
                                    refreshTrash()
                                    snackbarHostState.showSnackbar("Permanently deleted")
                                }
                            }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Recently Deleted?", fontWeight = FontWeight.Bold) },
            text = { Text("All ${trashItems.size} items in Recently Deleted will be permanently destroyed. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashDialog = false
                        scope.launch {
                            TrashManager.emptyTrash(context, isPrivate = false)
                            refreshTrash()
                            snackbarHostState.showSnackbar("Emptied Recently Deleted")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
            }
        )
    }
}
