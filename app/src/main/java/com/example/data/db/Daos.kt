package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaType = :type ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getHistoryByType(type: String, limit: Int = 10): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE uri = :uri LIMIT 1")
    suspend fun getHistoryByUri(uri: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entity: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE uri = :uri")
    suspend fun deleteHistoryByUri(uri: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAllHistory()
}

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uri = :uri)")
    fun isFavoriteFlow(uri: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()
}

@Dao
interface RecentArchivesDao {
    @Query("SELECT * FROM recent_archives ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentArchives(limit: Int = 10): Flow<List<RecentArchiveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArchive(entity: RecentArchiveEntity)

    @Query("DELETE FROM recent_archives WHERE uri = :uri")
    suspend fun deleteArchive(uri: String)

    @Query("DELETE FROM recent_archives")
    suspend fun clearArchives()
}
