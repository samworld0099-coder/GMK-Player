package com.example.ui.recent

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlaybackHistoryEntity
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.player.video.formatTime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentScreen(
    repository: MediaRepository,
    onOpenItem: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by repository.allHistory.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        if (historyList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${historyList.size} recent items",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showClearConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear History", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No recent history", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Played videos and audio will appear here", fontSize = 14.sp, color = Color.Gray)
                }
            }
        } else {
            val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("recent_history_list")
            ) {
                items(historyList, key = { it.uri }) { item ->
                    val isVideo = item.mediaType == "VIDEO"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenItem(
                                    MediaItem(
                                        uri = android.net.Uri.parse(item.uri),
                                        name = item.title,
                                        path = item.path,
                                        mediaType = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                                        durationMs = item.durationMs
                                    )
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isVideo) Color(0xFFFF5252).copy(alpha = 0.2f) else Color(0xFF448AFF).copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isVideo) Color(0xFFFF5252) else Color(0xFF448AFF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${formatTime(item.positionMs)} / ${formatTime(item.durationMs)} • ${dateFormat.format(Date(item.lastPlayedTimestamp))}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            scope.launch { repository.deleteHistory(item.uri) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All History?", fontWeight = FontWeight.Bold) },
            text = { Text("This will delete all playback history and progress.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        scope.launch { repository.clearAllHistory() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
