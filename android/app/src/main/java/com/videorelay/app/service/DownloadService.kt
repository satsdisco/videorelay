package com.videorelay.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.videorelay.app.data.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

/**
 * Foreground service for downloading videos for offline viewing.
 * Downloads are queued in Room and processed sequentially.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "videorelay_downloads"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.videorelay.app.DOWNLOAD_START"
        const val ACTION_CANCEL = "com.videorelay.app.DOWNLOAD_CANCEL"
        const val EXTRA_VIDEO_ID = "video_id"

        fun startDownload(context: Context, videoId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
            context.startForegroundService(intent)
        }
    }

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentVideoId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing download..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: return START_NOT_STICKY
                scope.launch { processDownload(videoId) }
            }
            ACTION_CANCEL -> {
                scope.cancel()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun processDownload(videoId: String) {
        currentVideoId = videoId
        val download = downloadRepository.getByVideoId(videoId) ?: return

        downloadRepository.updateStatus(videoId, "downloading")
        updateNotification("Downloading: ${download.title}")

        try {
            val downloadsDir = File(filesDir, "downloads").apply { mkdirs() }
            val outputFile = File(downloadsDir, "${videoId}.mp4")

            val request = Request.Builder()
                .url(download.videoUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                downloadRepository.updateStatus(videoId, "error")
                return
            }

            val body = response.body ?: run {
                downloadRepository.updateStatus(videoId, "error")
                return
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else 0

                        downloadRepository.updateStatus(
                            videoId, "downloading", progress
                        )
                        updateNotification("${download.title} — ${progress}%")
                    }
                }
            }

            downloadRepository.updateStatus(
                videoId, "complete",
                progress = 100,
                localPath = outputFile.absolutePath,
            )

            updateNotification("Download complete: ${download.title}")

            // Check for more queued downloads
            delay(2000)
            stopSelf()

        } catch (e: Exception) {
            downloadRepository.updateStatus(videoId, "error")
            updateNotification("Download failed: ${download.title}")
            delay(3000)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows download progress"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VideoRelay")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
