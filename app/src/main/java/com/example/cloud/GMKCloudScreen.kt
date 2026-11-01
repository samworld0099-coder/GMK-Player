package com.example.cloud

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.ui.theme.GMKBackgroundDark
import com.example.ui.theme.GMKElectricBlue
import com.example.ui.theme.GMKSurfaceDark
import com.example.ui.theme.GMKSurfaceVariantDark
import com.example.ui.theme.GMKTextDim
import com.example.ui.theme.GMKTextMuted
import com.example.ui.theme.GMKTextWhite
import com.example.ui.theme.GMKVerifiedGreen
import kotlinx.coroutines.launch
import java.io.File

data class CloudFileItem(
    val id: String,
    val name: String,
    val sizeFormatted: String,
    val dateFormatted: String,
    val type: MediaType,
    val isOffline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GMKCloudScreen(
    onOpenFile: (MediaItem) -> Unit = {},
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf("GMK Cloud") } // GMK Cloud, My Files, Shared
    var isConnected by remember { mutableStateOf(true) }
    var userEmail by remember { mutableStateOf("aman.khan134786@gmail.com") }

    var cloudItems by remember {
        mutableStateOf(
            listOf(
                CloudFileItem("1", "Jawan (2023).mkv", "2.1 GB", "12 Oct 2026", MediaType.VIDEO, true),
                CloudFileItem("2", "Vacation Photos.zip", "450 MB", "10 Oct 2026", MediaType.ARCHIVE, false),
                CloudFileItem("3", "Alvida Mere Dil.mp3", "9.8 MB", "8 Oct 2026", MediaType.AUDIO, true),
                CloudFileItem("4", "Documents.pdf", "2.4 MB", "5 Oct 2026", MediaType.DOCUMENT, true)
            )
        )
    }

    // SAF file picker for upload
    val uploadPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var fileName = "Uploaded_File"
            var fileSize = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (_: Exception) {}

            val ext = fileName.substringAfterLast('.', "").lowercase()
            val type = when (ext) {
                "mp4", "mkv", "webm", "avi", "mov" -> MediaType.VIDEO
                "mp3", "m4a", "flac", "wav", "aac" -> MediaType.AUDIO
                "jpg", "jpeg", "png", "webp", "gif" -> MediaType.IMAGE
                "zip", "rar", "7z", "tar", "gz" -> MediaType.ARCHIVE
                else -> MediaType.DOCUMENT
            }
            val formattedSize = if (fileSize > 0) com.example.transfer.TransferEngine.formatBytes(fileSize) else "1.5 MB"

            scope.launch {
                snackbarHostState.showSnackbar("Uploading \"$fileName\" ($formattedSize) to Google Drive...")
                cloudItems = listOf(
                    CloudFileItem(
                        id = System.currentTimeMillis().toString(),
                        name = fileName,
                        sizeFormatted = formattedSize,
                        dateFormatted = "Just now",
                        type = type,
                        isOffline = true
                    )
                ) + cloudItems
                snackbarHostState.showSnackbar("\"$fileName\" uploaded successfully to GMK Cloud")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = GMKTextWhite)
                    }
                },
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("GMK Player", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GMKTextWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(GMKVerifiedGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Verified", tint = Color.Black, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = GMKTextWhite)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = GMKTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GMKBackgroundDark)
            )
        },
        containerColor = GMKBackgroundDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pill Switcher Tabs: GMK Cloud | My Files | Shared
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("GMK Cloud", "My Files", "Shared").forEach { tabTitle ->
                        val isSelected = selectedTab == tabTitle
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) GMKElectricBlue else GMKSurfaceDark,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E283A)) else null,
                            modifier = Modifier.clickable { selectedTab = tabTitle }
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else GMKTextMuted,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Google Drive Integration Card (Image 3 Screen 7)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GMKSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Google Drive colorful icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F1E2E),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Google Drive",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isConnected) "Connected to Google Drive" else "Google Drive Disconnected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GMKTextWhite
                                )
                                Text(
                                    text = if (isConnected) userEmail else "Sign in to backup & sync files",
                                    fontSize = 11.sp,
                                    color = GMKTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1C2538),
                                modifier = Modifier.clickable {
                                    isConnected = !isConnected
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (isConnected) "Connected to Google Drive" else "Disconnected from Google Drive")
                                    }
                                }
                            ) {
                                Text(
                                    text = if (isConnected) "Disconnect" else "Connect",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isConnected) GMKTextMuted else GMKElectricBlue,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Storage Usage Meter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("12.4 GB / 100 GB used", fontSize = 12.sp, color = GMKTextWhite, fontWeight = FontWeight.Medium)
                            Text("12%", fontSize = 12.sp, color = GMKTextMuted, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { 0.12f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = GMKElectricBlue,
                            trackColor = Color(0xFF131D2D)
                        )
                    }
                }
            }

            // 4 Action Buttons: Upload | My Cloud Files | Shared with me | Offline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val actions = listOf(
                        Quadruple("Upload", Icons.Default.CloudUpload, Color(0xFF1E88E5)) {
                            uploadPicker.launch(arrayOf("*/*"))
                        },
                        Quadruple("My Cloud Files", Icons.Default.Folder, Color(0xFF00C853)) {
                            selectedTab = "My Files"
                        },
                        Quadruple("Shared with me", Icons.Default.Group, Color(0xFF7C4DFF)) {
                            selectedTab = "Shared"
                        },
                        Quadruple("Offline", Icons.Default.OfflinePin, Color(0xFFFF9100)) {
                            scope.launch { snackbarHostState.showSnackbar("3 files available for offline playback") }
                        }
                    )

                    actions.forEach { (label, icon, iconColor, onClick) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onClick() }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = iconColor,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = GMKTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Recent Cloud Files Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Cloud Files", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GMKTextWhite)
                    Text("See all >", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GMKElectricBlue)
                }
            }

            items(cloudItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dummyFile = File(context.cacheDir, item.name)
                            if (!dummyFile.exists()) {
                                try { dummyFile.createNewFile() } catch (_: Exception) {}
                            }
                            onOpenFile(
                                MediaItem(
                                    uri = android.net.Uri.fromFile(dummyFile),
                                    name = item.name,
                                    path = dummyFile.absolutePath,
                                    size = 0L,
                                    mediaType = item.type,
                                    isArchive = item.type == MediaType.ARCHIVE
                                )
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GMKSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF161E2E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (item.type) {
                                MediaType.VIDEO -> Color(0xFF7C4DFF).copy(alpha = 0.2f)
                                MediaType.AUDIO -> Color(0xFFE91E63).copy(alpha = 0.2f)
                                MediaType.ARCHIVE -> Color(0xFFFF9100).copy(alpha = 0.2f)
                                else -> Color(0xFF00C853).copy(alpha = 0.2f)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (item.type) {
                                        MediaType.VIDEO -> Icons.Default.Movie
                                        MediaType.AUDIO -> Icons.Default.MusicNote
                                        MediaType.ARCHIVE -> Icons.Default.Archive
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = when (item.type) {
                                        MediaType.VIDEO -> Color(0xFF7C4DFF)
                                        MediaType.AUDIO -> Color(0xFFE91E63)
                                        MediaType.ARCHIVE -> Color(0xFFFF9100)
                                        else -> Color(0xFF00C853)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GMKTextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${item.sizeFormatted} • ${item.dateFormatted}", fontSize = 11.sp, color = GMKTextMuted)
                        }

                        if (item.isOffline) {
                            Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = GMKVerifiedGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = GMKTextMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Promotional Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uploadPicker.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1729)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GMKElectricBlue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GMKElectricBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = GMKElectricBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upload files to GMK Cloud", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GMKTextWhite)
                            Text(
                                "Keep your videos, music, photos and documents safe in your Google Drive",
                                fontSize = 11.sp,
                                color = GMKTextMuted,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = GMKElectricBlue)
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
