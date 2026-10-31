package com.example.ui.favorites

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.filemanager.FileManagerHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FavoritesScreen(
    repository: MediaRepository,
    onOpenItem: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by repository.allFavorites.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    if (favorites.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No favorites yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Star files, songs, videos, or folders to access them quickly",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("favorites_list")
        ) {
            items(favorites, key = { it.path }) { fav ->
                val mediaType = try {
                    MediaType.valueOf(fav.mediaType)
                } catch (_: Exception) {
                    MediaType.OTHER
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val file = File(fav.path)
                            val item = if (file.exists()) {
                                FileManagerHelper.fileToMediaItem(file)
                            } else {
                                MediaItem(
                                    uri = android.net.Uri.parse(fav.uri),
                                    name = fav.title,
                                    path = fav.path,
                                    isDirectory = fav.isDirectory,
                                    mediaType = mediaType,
                                    mimeType = fav.mimeType
                                )
                            }
                            onOpenItem(item)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when {
                        fav.isDirectory -> Icons.Default.Folder
                        mediaType == MediaType.VIDEO -> Icons.Default.Movie
                        mediaType == MediaType.AUDIO -> Icons.Default.MusicNote
                        mediaType == MediaType.ARCHIVE -> Icons.Default.FolderZip
                        else -> Icons.Default.InsertDriveFile
                    }
                    val iconColor = when {
                        fav.isDirectory -> Color(0xFFFFA000)
                        mediaType == MediaType.VIDEO -> Color(0xFFFF5252)
                        mediaType == MediaType.AUDIO -> Color(0xFF448AFF)
                        mediaType == MediaType.ARCHIVE -> Color(0xFFFFAB00)
                        else -> Color.Gray
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = iconColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fav.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (fav.isDirectory) "Folder" else fav.path,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = {
                        scope.launch { repository.removeFavorite(fav.uri) }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Remove Favorite",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
