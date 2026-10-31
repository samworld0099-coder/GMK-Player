package com.example.feedback

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class FeedbackItem(
    val id: String,
    val type: String,
    val rating: Int, // 0 if not provided, or 1..5
    val message: String,
    val section: String,
    val email: String,
    val timestamp: Long,
    val appVersion: String
)

object FeedbackManager {

    private const val PREFS_NAME = "gmk_feedback_store"
    private const val KEY_FEEDBACK_LIST = "saved_feedback_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getSavedFeedback(context: Context): List<FeedbackItem> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_FEEDBACK_LIST, "[]") ?: "[]"
        val list = mutableListOf<FeedbackItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FeedbackItem(
                        id = obj.getString("id"),
                        type = obj.getString("type"),
                        rating = obj.optInt("rating", 0),
                        message = obj.getString("message"),
                        section = obj.optString("section", "General"),
                        email = obj.optString("email", ""),
                        timestamp = obj.getLong("timestamp"),
                        appVersion = obj.optString("appVersion", "1.0")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun saveFeedback(context: Context, item: FeedbackItem): Boolean {
        val current = getSavedFeedback(context).toMutableList()
        current.add(0, item)
        val capped = current.take(100)

        val array = JSONArray()
        for (f in capped) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("type", f.type)
            obj.put("rating", f.rating)
            obj.put("message", f.message)
            obj.put("section", f.section)
            obj.put("email", f.email)
            obj.put("timestamp", f.timestamp)
            obj.put("appVersion", f.appVersion)
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_FEEDBACK_LIST, array.toString()).apply()
        return true
    }

    @Synchronized
    fun clearFeedback(context: Context) {
        getPrefs(context).edit().remove(KEY_FEEDBACK_LIST).apply()
    }
}
