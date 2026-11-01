package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.launch

import com.example.ui.theme.AppThemeMode

@Composable
fun SettingsScreen(
    repository: MediaRepository,
    currentTheme: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeChanged: (AppThemeMode) -> Unit = {},
    onOpenStorageCleanup: () -> Unit = {},
    onOpenDonation: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenContactUs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var resumePlayback by remember { mutableStateOf(true) }
    var keepScreenAwake by remember { mutableStateOf(true) }
    var gesturesEnabled by remember { mutableStateOf(true) }
    var defaultSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var historyClearedNotice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Section: Appearance & Theme
        SettingsSectionHeader("Appearance & Theme")

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeChanged(AppThemeMode.SYSTEM) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == AppThemeMode.SYSTEM,
                        onClick = { onThemeChanged(AppThemeMode.SYSTEM) },
                        modifier = Modifier.testTag("theme_radio_system")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Follow System (Default)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Adapts automatically to system dark or light settings",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeChanged(AppThemeMode.LIGHT) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == AppThemeMode.LIGHT,
                        onClick = { onThemeChanged(AppThemeMode.LIGHT) },
                        modifier = Modifier.testTag("theme_radio_light")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Light Mode",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Clean white and light neutral grey background",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeChanged(AppThemeMode.DARK) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == AppThemeMode.DARK,
                        onClick = { onThemeChanged(AppThemeMode.DARK) },
                        modifier = Modifier.testTag("theme_radio_dark")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cinematic Dark (Default)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Deep navy cinematic background with electric blue accents",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeChanged(AppThemeMode.BLACK) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == AppThemeMode.BLACK,
                        onClick = { onThemeChanged(AppThemeMode.BLACK) },
                        modifier = Modifier.testTag("theme_radio_black")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AMOLED Pure Black",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "True pitch-black #000000 pixels for maximum battery savings",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeChanged(AppThemeMode.BLUE_ACCENT) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == AppThemeMode.BLUE_ACCENT,
                        onClick = { onThemeChanged(AppThemeMode.BLUE_ACCENT) },
                        modifier = Modifier.testTag("theme_radio_blue")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Electric Royal Blue",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Royal blue glowing surfaces inspired by GMK signature design",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Storage Management
        SettingsSectionHeader("Storage Management")

        SettingsClickableRow(
            icon = Icons.Default.Folder,
            title = "Clean Up Phone & Storage Analyzer",
            subtitle = "Review large files, old downloads, duplicate candidates, and app cache",
            onClick = onOpenStorageCleanup
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Video Playback
        SettingsSectionHeader("Playback & VLC Video Controls")

        SettingsSwitchRow(
            icon = Icons.Default.PlayCircle,
            title = "Resume playback",
            subtitle = "Remember last watched position and offer resume prompt",
            checked = resumePlayback,
            onCheckedChange = { resumePlayback = it }
        )

        SettingsSwitchRow(
            icon = Icons.Default.BrightnessMedium,
            title = "Keep screen awake",
            subtitle = "Prevent display sleep during video and audio playback",
            checked = keepScreenAwake,
            onCheckedChange = { keepScreenAwake = it }
        )

        SettingsSwitchRow(
            icon = Icons.Default.Gesture,
            title = "VLC gesture controls",
            subtitle = "Swipe vertically for brightness & volume, double-tap to seek",
            checked = gesturesEnabled,
            onCheckedChange = { gesturesEnabled = it }
        )

        SettingsClickableRow(
            icon = Icons.Default.Speed,
            title = "Default playback speed",
            subtitle = "${defaultSpeed}x",
            onClick = { showSpeedDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Archive Manager
        SettingsSectionHeader("Archive Management (ZArchiver Engine)")

        SettingsInfoRow(
            icon = Icons.Default.Security,
            title = "Path traversal protection",
            subtitle = "Active. Prevents Zip Slip and malicious directory extraction."
        )

        SettingsInfoRow(
            icon = Icons.Default.Folder,
            title = "Default extraction folder",
            subtitle = "Downloads/GMK_Extracted/"
        )

        SettingsInfoRow(
            icon = Icons.Default.Archive,
            title = "Supported formats",
            subtitle = "ZIP, 7z, TAR, GZ, TGZ, BZ2, TBZ2, XZ, TXZ"
        )

        // Section: Download Acceleration Engine
        val context = androidx.compose.ui.platform.LocalContext.current
        val downloadEngine = remember { com.example.browser.DownloadAccelerationEngine.getInstance(context) }
        var smartAccelEnabled by remember { mutableStateOf(downloadEngine.isSmartAccelerationEnabled()) }
        var dataSaverEnabled by remember { mutableStateOf(downloadEngine.isDataSaverEnabled()) }

        SettingsSectionHeader("Download Acceleration & Network")

        SettingsSwitchRow(
            icon = Icons.Default.Bolt,
            title = "Smart Download Acceleration",
            subtitle = if (smartAccelEnabled) "Active: Multi-segment parallel HTTP Range connections (2-4 streams on supported servers)" else "Disabled: Standard single-stream downloads",
            checked = smartAccelEnabled,
            onCheckedChange = {
                smartAccelEnabled = it
                downloadEngine.setSmartAccelerationEnabled(it)
            }
        )

        SettingsSwitchRow(
            icon = Icons.Default.Speed,
            title = "Data-Efficient Downloads",
            subtitle = if (dataSaverEnabled) "Active: Respect metered connections & avoid redundant preflight probes" else "Standard network utilization",
            checked = dataSaverEnabled,
            onCheckedChange = {
                dataSaverEnabled = it
                downloadEngine.setDataSaverEnabled(it)
            }
        )

        SettingsInfoRow(
            icon = Icons.Default.Folder,
            title = "Download directory",
            subtitle = "Downloads/GMK_Downloads/"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Privacy & Storage
        SettingsSectionHeader("History & Privacy")

        SettingsClickableRow(
            icon = Icons.Default.Delete,
            title = "Clear playback history & caches",
            subtitle = if (historyClearedNotice) "History cleared" else "Remove all watch logs and recent archives",
            iconColor = MaterialTheme.colorScheme.error,
            onClick = { showClearHistoryDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Support & Community
        SettingsSectionHeader("Support & Community")

        SettingsClickableRow(
            icon = Icons.Default.Favorite,
            title = "❤️ Donate / Support GMK",
            subtitle = "Support app development with Google Pay QR & UPI",
            iconColor = Color(0xFFD32F2F),
            onClick = onOpenDonation
        )

        SettingsClickableRow(
            icon = Icons.Default.Feedback,
            title = "💬 Feedback & Suggestions",
            subtitle = "Send bug reports, ideas, and feature requests",
            iconColor = MaterialTheme.colorScheme.primary,
            onClick = onOpenFeedback
        )

        SettingsClickableRow(
            icon = Icons.Default.Email,
            title = "📩 Contact Us",
            subtitle = "Email the developer & view UPI donation details",
            iconColor = MaterialTheme.colorScheme.secondary,
            onClick = onOpenContactUs
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section: About
        SettingsSectionHeader("About GMK Player")

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GMK Player v1.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Developer: GMK Studios",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Package: com.gmk.mediaplayer",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High-performance media playback engine supporting 4K UHD, 2K QHD, 1080p FHD, multi-track audio, subtitles, gesture seeking, encrypted private vault, archive management, and Wi-Fi file sharing.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Default Playback Speed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultSpeed = speed
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultSpeed == speed,
                                onClick = {
                                    defaultSpeed = speed
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${speed}x", fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Done") }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all recent files, playback history, and recent archives.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        scope.launch {
                            repository.clearAllHistory()
                            repository.clearRecentArchives()
                            historyClearedNotice = true
                        }
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = iconColor ?: MaterialTheme.colorScheme.onSurface
            )
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
}

@Composable
fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
}
