package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteEntity
import com.example.data.db.PlaybackHistoryEntity
import com.example.data.db.RecentArchiveEntity
import com.example.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

class MediaRepository(private val db: AppDatabase) {

    // History
    val allHistory: Flow<List<PlaybackHistoryEntity>> = db.playbackHistoryDao().getAllHistory()
    val videoHistory: Flow<List<PlaybackHistoryEntity>> = db.playbackHistoryDao().getHistoryByType("VIDEO")
    val audioHistory: Flow<List<PlaybackHistoryEntity>> = db.playbackHistoryDao().getHistoryByType("AUDIO")

    suspend fun getPlaybackPosition(uri: String): Long {
        val record = db.playbackHistoryDao().getHistoryByUri(uri)
        return record?.positionMs ?: 0L
    }

    suspend fun recordPlayback(
        uri: String,
        title: String,
        path: String,
        durationMs: Long,
        positionMs: Long,
        mediaType: String,
        completed: Boolean = false
    ) {
        db.playbackHistoryDao().upsertHistory(
            PlaybackHistoryEntity(
                uri = uri,
                title = title,
                path = path,
                durationMs = durationMs,
                positionMs = positionMs,
                mediaType = mediaType,
                lastPlayedTimestamp = System.currentTimeMillis(),
                completed = completed
            )
        )
    }

    suspend fun deleteHistory(uri: String) = db.playbackHistoryDao().deleteHistoryByUri(uri)
    suspend fun clearAllHistory() = db.playbackHistoryDao().clearAllHistory()

    // Favorites
    val allFavorites: Flow<List<FavoriteEntity>> = db.favoritesDao().getAllFavorites()

    fun isFavorite(uri: String): Flow<Boolean> = db.favoritesDao().isFavoriteFlow(uri)

    suspend fun toggleFavorite(item: MediaItem) {
        val uriStr = item.uri.toString()
        if (db.favoritesDao().isFavorite(uriStr)) {
            db.favoritesDao().removeFavorite(uriStr)
        } else {
            db.favoritesDao().addFavorite(
                FavoriteEntity(
                    uri = uriStr,
                    title = item.name,
                    path = item.path,
                    isDirectory = item.isDirectory,
                    mimeType = item.mimeType,
                    mediaType = item.mediaType.name
                )
            )
        }
    }

    suspend fun removeFavorite(uri: String) = db.favoritesDao().removeFavorite(uri)
    suspend fun clearFavorites() = db.favoritesDao().clearFavorites()

    // Recent Archives
    val recentArchives: Flow<List<RecentArchiveEntity>> = db.recentArchivesDao().getRecentArchives()

    suspend fun recordArchiveAccess(
        uri: String,
        name: String,
        path: String,
        format: String,
        entryCount: Int,
        size: Long
    ) {
        db.recentArchivesDao().upsertArchive(
            RecentArchiveEntity(
                uri = uri,
                name = name,
                path = path,
                format = format,
                entryCount = entryCount,
                size = size,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecentArchives() = db.recentArchivesDao().clearArchives()
}
