package com.example.browser

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserHistoryAndPrivacySheet(
    onDismiss: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    onCloseAllTabsRequested: () -> Unit,
    onRestoreTab: (RecentlyClosedTab) -> Unit,
    onClearCookies: () -> Unit,
    onClearCache: () -> Unit,
    onOpenClearAllBrowsingData: () -> Unit,
    openTabsCount: Int
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var browsingHistory by remember { mutableStateOf(BrowserHistoryManager.getBrowsingHistory(context)) }
    var searchHistory by remember { mutableStateOf(BrowserHistoryManager.getSearchHistory(context)) }
    var recentlyClosedTabs by remember { mutableStateOf(BrowserHistoryManager.getRecentlyClosedTabs(context)) }

    // Dialog states
    var showClearBrowsingDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }
    var showClearClosedTabsDialog by remember { mutableStateOf(false) }
    var showClearCookiesConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    fun refreshData() {
        browsingHistory = BrowserHistoryManager.getBrowsingHistory(context)
        searchHistory = BrowserHistoryManager.getSearchHistory(context)
        recentlyClosedTabs = BrowserHistoryManager.getRecentlyClosedTabs(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("browser_history_privacy_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "History & Privacy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Browsing (${browsingHistory.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Search (${searchHistory.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tabs & Data", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                // ==========================================
                // TAB 0: BROWSING HISTORY
                // ==========================================
                0 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Page Visits (${browsingHistory.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { showClearBrowsingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(16.dp),
                                enabled = browsingHistory.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear History", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Search queries, cookies, tabs, and downloaded files remain safe and untouched.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (browsingHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No browsing history entries", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(browsingHistory, key = { it.id }) { item ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onDismiss()
                                                    onNavigateToUrl(item.url)
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(item.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(formatTimestamp(item.timestamp), fontSize = 10.sp, color = Color.LightGray)
                                            }
                                            IconButton(
                                                onClick = {
                                                    BrowserHistoryManager.removeBrowsingHistoryItem(context, item.id)
                                                    refreshData()
                                                }
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete item", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 1: SEARCH HISTORY
                // ==========================================
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Search Queries (${searchHistory.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { showClearSearchDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(16.dp),
                                enabled = searchHistory.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Search History", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Clearing removes search suggestions only. Open tabs, bookmarks, cookies, and downloads are never deleted.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (searchHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No saved search queries", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(searchHistory, key = { it.id }) { item ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onDismiss()
                                                    onNavigateToUrl(item.query)
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.query, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                Text(formatTimestamp(item.timestamp), fontSize = 10.sp, color = Color.Gray)
                                            }
                                            IconButton(
                                                onClick = {
                                                    BrowserHistoryManager.removeSearchQuery(context, item.id)
                                                    refreshData()
                                                }
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete search", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 2: TABS & SITE DATA
                // ==========================================
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Close All Tabs Action Card
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Open Tabs ($openTabsCount)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Close all tabs and reset to a clean home tab", fontSize = 11.sp, color = Color.Gray)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onCloseAllTabsRequested()
                                        refreshData()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Close All Tabs", fontSize = 11.sp)
                                }
                            }
                        }

                        // Recently Closed Tabs
                        if (recentlyClosedTabs.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recently Closed (${recentlyClosedTabs.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                TextButton(onClick = { showClearClosedTabsDialog = true }) {
                                    Text("Clear", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(recentlyClosedTabs) { closedTab ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(closedTab.title, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        TextButton(onClick = {
                                            onDismiss()
                                            onRestoreTab(closedTab)
                                        }) {
                                            Text("Restore", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Cookies & Cache Independent Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showClearCookiesConfirm = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear Cookies", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { showClearCacheConfirm = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear Cache", fontSize = 11.sp)
                            }
                        }

                        // Full Clear Browsing Data Shortcut
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenClearAllBrowsingData()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Selective Clear All Browsing Data", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // CLEAR BROWSING HISTORY DIALOG (Time Range)
    // ==========================================
    if (showClearBrowsingDialog) {
        var selectedRange by remember { mutableStateOf(HistoryTimeRange.ALL_TIME) }
        AlertDialog(
            onDismissRequest = { showClearBrowsingDialog = false },
            icon = { Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Browsing History") },
            text = {
                Column {
                    Text("Select time range to remove matching visited pages:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    HistoryTimeRange.values().forEach { range ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRange = range }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRange == range,
                                onClick = { selectedRange = range }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(range.label, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Note: Current open tabs, bookmarks, and downloaded files will remain intact.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        BrowserHistoryManager.clearBrowsingHistory(context, selectedRange)
                        showClearBrowsingDialog = false
                        refreshData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearBrowsingDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // CLEAR SEARCH HISTORY DIALOG
    // ==========================================
    if (showClearSearchDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchDialog = false },
            icon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Clear Search History?") },
            text = {
                Text(
                    "Are you sure you want to remove all saved search queries? Suggestions will be cleared immediately. Open tabs, bookmarks, and downloads will not be changed.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        BrowserHistoryManager.clearSearchHistory(context)
                        showClearSearchDialog = false
                        refreshData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Queries")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // CLEAR RECENTLY CLOSED TABS DIALOG
    // ==========================================
    if (showClearClosedTabsDialog) {
        AlertDialog(
            onDismissRequest = { showClearClosedTabsDialog = false },
            title = { Text("Clear Recently Closed Tabs?") },
            text = { Text("Remove saved closed tabs history from this device.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        BrowserHistoryManager.clearRecentlyClosedTabs(context)
                        showClearClosedTabsDialog = false
                        refreshData()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearClosedTabsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // CLEAR COOKIES CONFIRMATION
    // ==========================================
    if (showClearCookiesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCookiesConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Cookies and Site Data?") },
            text = {
                Text(
                    "This will sign you out of most websites and clear local website data. Open tabs and bookmarks will not be deleted.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCookiesConfirm = false
                        onClearCookies()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Cookies")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCookiesConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // CLEAR CACHE CONFIRMATION
    // ==========================================
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear Cached Images and Files?") },
            text = {
                Text(
                    "Frees temporary cache space. Some sites may take slightly longer to load on your next visit.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheConfirm = false
                        onClearCache()
                    }
                ) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ==========================================
// CLOSE ALL TABS CONFIRMATION DIALOG
// ==========================================
@Composable
fun CloseAllTabsDialog(
    tabCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Tab, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Close All Tabs?") },
        text = {
            Column {
                Text(
                    "This will close all $tabCount open browser tabs and open a clean new tab. Your bookmarks and downloaded files are safe and will not be touched.",
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Closed tabs will be saved to 'Recently Closed Tabs' so you can restore them if needed.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_close_all_tabs_button")
            ) {
                Text("Close All Tabs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==========================================
// CLEAR ALL BROWSING DATA (SELECTIVE ONE-CLICK)
// ==========================================
@Composable
fun ClearAllBrowsingDataDialog(
    onConfirm: (ClearBrowsingDataOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var clearSearch by remember { mutableStateOf(true) }
    var clearBrowsing by remember { mutableStateOf(true) }
    var closeTabs by remember { mutableStateOf(false) }
    var clearCookies by remember { mutableStateOf(false) }
    var clearCache by remember { mutableStateOf(true) }
    var clearClosedTabs by remember { mutableStateOf(false) }

    val allSelected = clearSearch && clearBrowsing && closeTabs && clearCookies && clearCache && clearClosedTabs

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Clear Browsing Data", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select items to clear:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = {
                            val newVal = !allSelected
                            clearSearch = newVal
                            clearBrowsing = newVal
                            closeTabs = newVal
                            clearCookies = newVal
                            clearCache = newVal
                            clearClosedTabs = newVal
                        }
                    ) {
                        Text(if (allSelected) "Deselect All" else "Select All", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                ClearOptionRow(
                    title = "Search History",
                    subtitle = "Removes saved search queries and search suggestions",
                    checked = clearSearch,
                    onCheckedChange = { clearSearch = it }
                )

                ClearOptionRow(
                    title = "Browsing History",
                    subtitle = "Clears visits from history",
                    checked = clearBrowsing,
                    onCheckedChange = { clearBrowsing = it }
                )

                ClearOptionRow(
                    title = "Open Tabs",
                    subtitle = "Closes open tabs and resets to clean new tab",
                    checked = closeTabs,
                    onCheckedChange = { closeTabs = it }
                )

                ClearOptionRow(
                    title = "Cookies and Site Data",
                    subtitle = "May sign you out of websites",
                    warning = true,
                    checked = clearCookies,
                    onCheckedChange = { clearCookies = it }
                )

                ClearOptionRow(
                    title = "Cached Images and Files",
                    subtitle = "Frees temporary web cache",
                    checked = clearCache,
                    onCheckedChange = { clearCache = it }
                )

                ClearOptionRow(
                    title = "Recently Closed Tabs",
                    subtitle = "Erases restored tab history",
                    checked = clearClosedTabs,
                    onCheckedChange = { clearClosedTabs = it }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Bookmarks and downloaded files are safe and never deleted.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        ClearBrowsingDataOptions(
                            clearSearchHistory = clearSearch,
                            clearBrowsingHistory = clearBrowsing,
                            closeOpenTabs = closeTabs,
                            clearCookies = clearCookies,
                            clearCache = clearCache,
                            clearRecentlyClosedTabs = clearClosedTabs
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("clear_selected_data_button")
            ) {
                Text("Clear Selected Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ClearOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    warning: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (warning && checked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(ts))
}
