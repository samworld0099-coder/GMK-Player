package com.example.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.FileProvider
import com.example.data.model.MediaItem
import com.example.filemanager.FileManagerHelper
import com.example.transfer.TransferEngine
import java.io.File

/**
 * Chrome-Like Download Manager Bottom Sheet with categorized tabs:
 * [All, Active, Paused, Completed, Failed], real-time speed in MB/s, ETA calculation,
 * pause, resume, retry, cancel, share, and open in GMK Player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromeDownloadsSheet(
    engine: DownloadAccelerationEngine,
    userAgent: String?,
    onOpenDownloadedMedia: (MediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allTasks by engine.tasksFlow.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Active", "Paused", "Completed", "Failed")

    val filteredTasks = remember(allTasks, selectedTab) {
        when (selectedTab) {
            1 -> allTasks.filter { it.status == DownloadTaskStatus.DOWNLOADING || it.status == DownloadTaskStatus.PENDING }
            2 -> allTasks.filter { it.status == DownloadTaskStatus.PAUSED }
            3 -> allTasks.filter { it.status == DownloadTaskStatus.COMPLETED }
            4 -> allTasks.filter { it.status == DownloadTaskStatus.FAILED || it.status == DownloadTaskStatus.CANCELLED }
            else -> allTasks
        }
    }

    val isAccelerationActive = remember(engine) { engine.isSmartAccelerationEnabled() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("chrome_downloads_sheet")
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Downloads", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (isAccelerationActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "Turbo Range",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Downloads/GMK_Downloads/",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    val count = when (index) {
                        1 -> allTasks.count { it.status == DownloadTaskStatus.DOWNLOADING || it.status == DownloadTaskStatus.PENDING }
                        2 -> allTasks.count { it.status == DownloadTaskStatus.PAUSED }
                        3 -> allTasks.count { it.status == DownloadTaskStatus.COMPLETED }
                        4 -> allTasks.count { it.status == DownloadTaskStatus.FAILED || it.status == DownloadTaskStatus.CANCELLED }
                        else -> allTasks.size
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                if (count > 0) "$title ($count)" else title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No ${tabs[selectedTab].lowercase()} downloads",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        DownloadItemCard(
                            task = task,
                            onPause = { engine.pauseDownload(task.id) },
                            onResume = { engine.resumeDownload(task.id, userAgent) },
                            onRetry = { engine.retryDownload(task.id, userAgent) },
                            onCancel = { engine.cancelDownload(task.id) },
                            onDelete = { engine.deleteDownload(task.id, deleteFileOnDisk = true) },
                            onOpen = {
                                val file = File(task.destinationPath)
                                if (file.exists()) {
                                    onDismiss()
                                    onOpenDownloadedMedia(FileManagerHelper.fileToMediaItem(file))
                                }
                            },
                            onShare = {
                                val file = File(task.destinationPath)
                                if (file.exists()) {
                                    shareFile(context, file)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DownloadItemCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.fileName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(status = task.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // In-Progress details (Downloading / Paused)
            if (task.status == DownloadTaskStatus.DOWNLOADING || task.status == DownloadTaskStatus.PAUSED) {
                val progress = if (task.totalBytes > 0) {
                    (task.downloadedBytes.toFloat() / task.totalBytes).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val downloadedStr = TransferEngine.formatBytes(task.downloadedBytes)
                    val totalStr = if (task.totalBytes > 0) TransferEngine.formatBytes(task.totalBytes) else "Unknown"
                    Text(
                        "$downloadedStr / $totalStr (${(progress * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (task.status == DownloadTaskStatus.DOWNLOADING) {
                        val speedStr = TransferEngine.formatSpeed(task.speedBps)
                        val etaStr = calculateEta(task.downloadedBytes, task.totalBytes, task.speedBps)
                        Text(
                            "$speedStr • ETA: $etaStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (task.status == DownloadTaskStatus.DOWNLOADING) {
                        TextButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 12.sp)
                        }
                    } else {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onCancel) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (task.status == DownloadTaskStatus.COMPLETED) {
                // Completed Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${TransferEngine.formatBytes(task.totalBytes)} • Downloaded",
                        fontSize = 12.sp,
                        color = Color(0xFF1E8E3E),
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = onOpen,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Open in GMK", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Failed or Cancelled Card
                task.errorMessage?.let { err ->
                    Text(
                        text = "Error: $err",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadTaskStatus) {
    val (bgColor, textColor, label) = when (status) {
        DownloadTaskStatus.COMPLETED -> Triple(Color(0xFFE6F4EA), Color(0xFF137333), "Completed")
        DownloadTaskStatus.DOWNLOADING -> Triple(Color(0xFFE8F0FE), Color(0xFF1A73E8), "Downloading")
        DownloadTaskStatus.PAUSED -> Triple(Color(0xFFF1F3F4), Color(0xFF5F6368), "Paused")
        DownloadTaskStatus.FAILED -> Triple(Color(0xFFFCE8E6), Color(0xFFC5221F), "Failed")
        DownloadTaskStatus.CANCELLED -> Triple(Color(0xFFF1F3F4), Color(0xFF70757A), "Cancelled")
        DownloadTaskStatus.PENDING -> Triple(Color(0xFFFEF7E0), Color(0xFFB06000), "Queued")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun calculateEta(downloaded: Long, total: Long, speedBps: Long): String {
    if (speedBps <= 0L || total <= downloaded) return "--"
    val remainingBytes = total - downloaded
    val remainingSec = remainingBytes / speedBps
    return when {
        remainingSec < 60 -> "${remainingSec}s"
        remainingSec < 3600 -> "${remainingSec / 60}m ${remainingSec % 60}s"
        else -> "${remainingSec / 3600}h ${(remainingSec % 3600) / 60}m"
    }
}

private fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share file"))
    } catch (_: Exception) {}
}
