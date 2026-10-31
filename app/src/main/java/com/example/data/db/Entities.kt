package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val path: String,
    val durationMs: Long,
    val positionMs: Long,
    val mediaType: String, // "VIDEO" or "AUDIO"
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val path: String,
    val isDirectory: Boolean,
    val mimeType: String?,
    val mediaType: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_archives")
data class RecentArchiveEntity(
    @PrimaryKey val uri: String,
    val name: String,
    val path: String,
    val format: String,
    val entryCount: Int,
    val size: Long,
    val timestamp: Long = System.currentTimeMillis()
)
