package com.example.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    isIncognito: Boolean,
    onToggleIncognito: (Boolean) -> Unit,
    onOpenVpn: () -> Unit,
    onOpenTor: () -> Unit,
    onOpenHistoryAndPrivacy: () -> Unit,
    onOpenClearBrowsingData: () -> Unit,
    searchEngine: String,
    onSelectSearchEngine: (String) -> Unit,
    isDesktopSite: Boolean,
    onToggleDesktopSite: (Boolean) -> Unit,
    enableFloatingAssistant: Boolean,
    onToggleFloatingAssistant: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    tabCount: Int,
    bookmarkCount: Int,
    activeDownloadCount: Int
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .testTag("browser_hamburger_menu_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Browser Menu",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "GMK Web",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: NAVIGATION
            MenuSectionHeader(title = "Navigation")

            MenuItemRow(
                icon = Icons.Default.Add,
                title = "New Tab",
                subtitle = "Open a clean browsing session",
                onClick = {
                    onDismiss()
                    onNewTab()
                }
            )

            MenuItemRow(
                icon = Icons.Default.Tab,
                title = "Tabs",
                subtitle = "$tabCount tab${if (tabCount > 1) "s" else ""} open",
                badgeCount = tabCount,
                onClick = {
                    onDismiss()
                    onOpenTabs()
                }
            )

            MenuItemRow(
                icon = Icons.Default.Bookmark,
                title = "Bookmarks",
                subtitle = "$bookmarkCount saved bookmark${if (bookmarkCount > 1) "s" else ""}",
                onClick = {
                    onDismiss()
                    onOpenBookmarks()
                }
            )

            MenuItemRow(
                icon = Icons.Default.Download,
                title = "Downloads",
                subtitle = "Saved to Downloads/GMK_Downloads/",
                badgeCount = activeDownloadCount,
                onClick = {
                    onDismiss()
                    onOpenDownloads()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // SECTION 2: PRIVACY & SECURITY
            MenuSectionHeader(title = "Privacy & Security")

            // Incognito Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isIncognito) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Incognito Mode", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Do not save history or persistent cookies", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Switch(
                    checked = isIncognito,
                    onCheckedChange = { onToggleIncognito(it) },
                    modifier = Modifier.testTag("toggle_incognito_mode")
                )
            }

            MenuItemRow(
                icon = Icons.Default.VpnLock,
                title = "VPN Connection",
                subtitle = "Not connected • Route via Android VPN Settings",
                onClick = {
                    onDismiss()
                    onOpenVpn()
                }
            )

            MenuItemRow(
                icon = Icons.Default.Shield,
                title = "Tor Network Setup",
                subtitle = "Local SOCKS5 proxy (127.0.0.1:9050 / Orbot)",
                onClick = {
                    onDismiss()
                    onOpenTor()
                }
            )

            MenuItemRow(
                icon = Icons.Default.History,
                title = "History & Privacy",
                subtitle = "Manage search history, browsing visits & tabs",
                highlight = true,
                onClick = {
                    onDismiss()
                    onOpenHistoryAndPrivacy()
                }
            )

            MenuItemRow(
                icon = Icons.Default.Delete,
                title = "Clear Browsing Data",
                subtitle = "Selective removal of history, cookies, tabs & cache",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismiss()
                    onOpenClearBrowsingData()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // SECTION 3: SETTINGS
            MenuSectionHeader(title = "Settings")

            // Search Engine Picker
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text("Search Engine", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Google", "DuckDuckGo", "Bing").forEach { engine ->
                        val selected = searchEngine == engine
                        OutlinedButton(
                            onClick = { onSelectSearchEngine(engine) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Text(
                                text = engine,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Desktop Site Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isDesktopSite) Icons.Default.Computer else Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Desktop Site", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Request desktop computer layouts", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Switch(
                    checked = isDesktopSite,
                    onCheckedChange = { onToggleDesktopSite(it) },
                    modifier = Modifier.testTag("toggle_desktop_site")
                )
            }

            // Floating Download Assistant Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Floating Download Assistant", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Draggable capture button in Web view only", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Switch(
                    checked = enableFloatingAssistant,
                    onCheckedChange = { onToggleFloatingAssistant(it) },
                    modifier = Modifier.testTag("toggle_floating_assistant")
                )
            }

            MenuItemRow(
                icon = Icons.Default.Settings,
                title = "Browser Settings",
                subtitle = "Cache, user agent, and security options",
                onClick = {
                    onDismiss()
                    onOpenSettings()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeCount: Int = 0,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (badgeCount > 0) {
            BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
        } else {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
