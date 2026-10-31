package com.example.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.files.getMediaIcon
import com.example.ui.files.getMediaTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    allItems: List<MediaItem>,
    onOpenItem: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    BackHandler { onBack() }

    val filteredList = remember(query, selectedFilter, allItems) {
        allItems.filter { item ->
            val matchesQuery = query.isEmpty() || item.name.contains(query, ignoreCase = true) || item.extension.contains(query, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "VIDEO" -> item.mediaType == MediaType.VIDEO
                "AUDIO" -> item.mediaType == MediaType.AUDIO
                "ARCHIVE" -> item.isArchive || item.mediaType == MediaType.ARCHIVE
                "DOCUMENT" -> item.mediaType == MediaType.DOCUMENT
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search files, videos, music...") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .testTag("search_text_input"),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    "ALL" to "All Files",
                    "VIDEO" to "Videos",
                    "AUDIO" to "Music",
                    "ARCHIVE" to "Archives",
                    "DOCUMENT" to "Documents"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (query.isEmpty()) "Search your media library" else "No matching results found",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag("search_results_list")) {
                    items(filteredList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenItem(item) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getMediaIcon(item),
                                contentDescription = null,
                                tint = getMediaTint(item),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.formattedSize} • ${item.mediaType.name}",
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
