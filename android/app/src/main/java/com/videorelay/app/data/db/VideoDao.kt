package com.videorelay.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getRecentVideos(limit: Int = 200): List<VideoEntity>

    @Query("SELECT * FROM videos ORDER BY zapCount DESC LIMIT :limit")
    suspend fun getMostZapped(limit: Int = 50): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE isShort = 1 ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getShorts(limit: Int = 100): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE pubkey IN (:pubkeys) ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getByAuthors(pubkeys: List<String>, limit: Int = 100): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE tags LIKE '%' || :tag || '%' ORDER BY publishedAt DESC LIMIT :limit")
    suspend fun getByTag(tag: String, limit: Int = 100): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE cachedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'complete' ORDER BY createdAt DESC")
    suspend fun getCompleted(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getByVideoId(videoId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, progress = :progress, localPath = :localPath WHERE videoId = :videoId")
    suspend fun updateStatus(videoId: String, status: String, progress: Int = 0, localPath: String = "")

    @Delete
    suspend fun delete(download: DownloadEntity)
}

@Dao
interface ViewHistoryDao {
    @Query("SELECT * FROM view_history ORDER BY watchedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<ViewHistoryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM view_history WHERE videoId = :videoId)")
    suspend fun hasWatched(videoId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ViewHistoryEntity)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE pubkey = :pubkey")
    suspend fun getByPubkey(pubkey: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE pubkey IN (:pubkeys)")
    suspend fun getByPubkeys(pubkeys: List<String>): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Query("DELETE FROM profiles WHERE fetchedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
