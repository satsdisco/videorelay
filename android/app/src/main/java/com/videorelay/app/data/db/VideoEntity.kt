package com.videorelay.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.videorelay.app.domain.model.Video

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val pubkey: String,
    val title: String,
    val summary: String,
    val thumbnail: String,
    val videoUrl: String,
    val duration: String,
    val durationSeconds: Int,
    val publishedAt: Long,
    val tags: String, // comma-separated
    val zapCount: Int = 0,
    val isShort: Boolean = false,
    val kind: Int = 21,
    val cachedAt: Long = System.currentTimeMillis(),
) {
    fun toDomain() = Video(
        id = id,
        pubkey = pubkey,
        title = title,
        summary = summary,
        thumbnail = thumbnail,
        videoUrl = videoUrl,
        duration = duration,
        durationSeconds = durationSeconds,
        publishedAt = publishedAt,
        tags = tags.split(",").filter { it.isNotBlank() },
        zapCount = zapCount,
        isShort = isShort,
        kind = kind,
    )

    companion object {
        fun from(video: Video) = VideoEntity(
            id = video.id,
            pubkey = video.pubkey,
            title = video.title,
            summary = video.summary,
            thumbnail = video.thumbnail,
            videoUrl = video.videoUrl,
            duration = video.duration,
            durationSeconds = video.durationSeconds,
            publishedAt = video.publishedAt,
            tags = video.tags.joinToString(","),
            zapCount = video.zapCount,
            isShort = video.isShort,
            kind = video.kind,
        )
    }
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnail: String,
    val videoUrl: String,
    val localPath: String = "",
    val status: String = "pending", // pending, downloading, complete, error
    val progress: Int = 0,
    val fileSize: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "view_history")
data class ViewHistoryEntity(
    @PrimaryKey val videoId: String,
    val watchedAt: Long = System.currentTimeMillis(),
    val watchedSeconds: Int = 0,
    val totalSeconds: Int = 0,
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val pubkey: String,
    val name: String = "",
    val displayName: String = "",
    val picture: String = "",
    val banner: String = "",
    val about: String = "",
    val lud16: String = "",
    val lud06: String = "",
    val nip05: String = "",
    val fetchedAt: Long = System.currentTimeMillis(),
)
