package com.videorelay.app.data.repository

import com.videorelay.app.data.db.DownloadDao
import com.videorelay.app.data.db.DownloadEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
) {
    fun observeDownloads(): Flow<List<DownloadEntity>> = downloadDao.observeAll()

    suspend fun getCompleted() = downloadDao.getCompleted()

    suspend fun getByVideoId(videoId: String) = downloadDao.getByVideoId(videoId)

    suspend fun queueDownload(videoId: String, title: String, thumbnail: String, videoUrl: String) {
        downloadDao.insert(
            DownloadEntity(
                videoId = videoId,
                title = title,
                thumbnail = thumbnail,
                videoUrl = videoUrl,
                status = "pending",
            )
        )
    }

    suspend fun updateStatus(videoId: String, status: String, progress: Int = 0, localPath: String = "") {
        downloadDao.updateStatus(videoId, status, progress, localPath)
    }

    suspend fun delete(download: DownloadEntity) {
        downloadDao.delete(download)
    }
}
