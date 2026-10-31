package com.example.browser

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

enum class HistoryTimeRange(val label: String, val durationMs: Long) {
    LAST_HOUR("Last hour", 3600_000L),
    LAST_24_HOURS("Last 24 hours", 86400_000L),
    LAST_7_DAYS("Last 7 days", 7 * 86400_000L),
    ALL_TIME("All time", Long.MAX_VALUE)
}

data class SearchHistoryItem(
    val id: String,
    val query: String,
    val timestamp: Long
)

data class BrowsingHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val timestamp: Long
)

data class RecentlyClosedTab(
    val id: String,
    val title: String,
    val url: String,
    val closedAt: Long
)

data class ClearBrowsingDataOptions(
    val clearSearchHistory: Boolean = true,
    val clearBrowsingHistory: Boolean = true,
    val closeOpenTabs: Boolean = false,
    val clearCookies: Boolean = false,
    val clearCache: Boolean = true,
    val clearRecentlyClosedTabs: Boolean = false
)

object BrowserHistoryManager {

    private const val PREFS_NAME = "gmk_browser_history_store"
    private const val KEY_SEARCH_HISTORY = "search_history_json"
    private const val KEY_BROWSING_HISTORY = "browsing_history_json"
    private const val KEY_CLOSED_TABS = "closed_tabs_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ==========================================
    // SEARCH HISTORY (Queries only)
    // ==========================================

    @Synchronized
    fun getSearchHistory(context: Context): List<SearchHistoryItem> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_SEARCH_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<SearchHistoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SearchHistoryItem(
                        id = obj.optString("id", "${obj.optLong("timestamp")}_$i"),
                        query = obj.getString("query"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun addSearchQuery(context: Context, query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val current = getSearchHistory(context).toMutableList()
        // Deduplicate query
        current.removeAll { it.query.equals(clean, ignoreCase = true) }
        current.add(0, SearchHistoryItem(id = "search_${System.currentTimeMillis()}", query = clean, timestamp = System.currentTimeMillis()))
        // Cap to 200 items
        val capped = current.take(200)
        saveSearchHistory(context, capped)
    }

    @Synchronized
    fun removeSearchQuery(context: Context, id: String) {
        val current = getSearchHistory(context).toMutableList()
        current.removeAll { it.id == id }
        saveSearchHistory(context, current)
    }

    @Synchronized
    fun clearSearchHistory(context: Context) {
        saveSearchHistory(context, emptyList())
    }

    private fun saveSearchHistory(context: Context, items: List<SearchHistoryItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("query", item.query)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SEARCH_HISTORY, array.toString()).apply()
    }

    // ==========================================
    // BROWSING HISTORY (Visits: Title + URL + Timestamp)
    // ==========================================

    @Synchronized
    fun getBrowsingHistory(context: Context): List<BrowsingHistoryItem> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_BROWSING_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<BrowsingHistoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BrowsingHistoryItem(
                        id = obj.optString("id", "${obj.optLong("timestamp")}_$i"),
                        title = obj.optString("title", "Web"),
                        url = obj.getString("url"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun addBrowsingHistory(context: Context, title: String, url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || cleanUrl == "about:blank") return
        val current = getBrowsingHistory(context).toMutableList()
        // Deduplicate recent same url if visited within 5 seconds
        val now = System.currentTimeMillis()
        val existingIndex = current.indexOfFirst { it.url == cleanUrl }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        }
        current.add(0, BrowsingHistoryItem(
            id = "hist_${now}",
            title = if (title.isNotBlank() && title != "Web") title else cleanUrl,
            url = cleanUrl,
            timestamp = now
        ))
        val capped = current.take(500)
        saveBrowsingHistory(context, capped)
    }

    @Synchronized
    fun removeBrowsingHistoryItem(context: Context, id: String) {
        val current = getBrowsingHistory(context).toMutableList()
        current.removeAll { it.id == id }
        saveBrowsingHistory(context, current)
    }

    @Synchronized
    fun clearBrowsingHistory(context: Context, timeRange: HistoryTimeRange = HistoryTimeRange.ALL_TIME): Int {
        val current = getBrowsingHistory(context)
        val now = System.currentTimeMillis()
        val remaining = if (timeRange == HistoryTimeRange.ALL_TIME) {
            emptyList()
        } else {
            val cutoff = now - timeRange.durationMs
            current.filter { it.timestamp < cutoff }
        }
        val removedCount = current.size - remaining.size
        saveBrowsingHistory(context, remaining)
        return removedCount
    }

    private fun saveBrowsingHistory(context: Context, items: List<BrowsingHistoryItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("timestamp", item.timestamp)
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_BROWSING_HISTORY, array.toString()).apply()
    }

    // ==========================================
    // RECENTLY CLOSED TABS
    // ==========================================

    @Synchronized
    fun getRecentlyClosedTabs(context: Context): List<RecentlyClosedTab> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_CLOSED_TABS, "[]") ?: "[]"
        val list = mutableListOf<RecentlyClosedTab>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RecentlyClosedTab(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        closedAt = obj.getLong("closedAt")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.closedAt }
    }

    @Synchronized
    fun addRecentlyClosedTab(context: Context, tab: BrowserTab) {
        if (tab.url.isBlank() || tab.url == "about:blank") return
        val current = getRecentlyClosedTabs(context).toMutableList()
        current.add(0, RecentlyClosedTab(
            id = "closed_${System.currentTimeMillis()}",
            title = tab.title,
            url = tab.url,
            closedAt = System.currentTimeMillis()
        ))
        val capped = current.take(30)
        saveRecentlyClosedTabs(context, capped)
    }

    @Synchronized
    fun removeRecentlyClosedTab(context: Context, id: String) {
        val current = getRecentlyClosedTabs(context).toMutableList()
        current.removeAll { it.id == id }
        saveRecentlyClosedTabs(context, current)
    }

    @Synchronized
    fun clearRecentlyClosedTabs(context: Context) {
        saveRecentlyClosedTabs(context, emptyList())
    }

    private fun saveRecentlyClosedTabs(context: Context, items: List<RecentlyClosedTab>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("url", item.url)
            obj.put("closedAt", item.closedAt)
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_CLOSED_TABS, array.toString()).apply()
    }

    // ==========================================
    // COOKIES & CACHE
    // ==========================================

    fun clearCookies(onComplete: (() -> Unit)? = null) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
            onComplete?.invoke()
        }
    }

    fun clearCache(webView: WebView?) {
        webView?.clearCache(true)
    }

    // ==========================================
    // CLEAR ALL BROWSING DATA (Selective Batch)
    // ==========================================

    fun clearBrowsingData(
        context: Context,
        options: ClearBrowsingDataOptions,
        webView: WebView?,
        onComplete: () -> Unit
    ) {
        if (options.clearSearchHistory) {
            clearSearchHistory(context)
        }
        if (options.clearBrowsingHistory) {
            clearBrowsingHistory(context, HistoryTimeRange.ALL_TIME)
        }
        if (options.clearRecentlyClosedTabs) {
            clearRecentlyClosedTabs(context)
        }
        if (options.clearCache) {
            clearCache(webView)
        }
        if (options.clearCookies) {
            clearCookies {
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    // ==========================================
    // VARIATIONS SEED CLEANUP
    // ==========================================

    fun cleanupCorruptVariationsSeed(context: Context) {
        try {
            val appDataDir = context.dataDir
            val webViewDirs = appDataDir.listFiles { file -> file.isDirectory && file.name.startsWith("app_webview") }
            webViewDirs?.forEach { dir ->
                val seedFiles = listOf("variations_seed", "variations_seed_new", "Variations Seed", "Variations Seed New")
                for (seedName in seedFiles) {
                    val seedFile = java.io.File(dir, seedName)
                    // If file is empty or corrupted placeholder without valid signature, purge it so Chromium starts clean
                    if (seedFile.exists() && seedFile.length() < 64L) {
                        seedFile.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
