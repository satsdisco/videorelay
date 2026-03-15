package com.videorelay.app.data.repository

import com.videorelay.app.data.db.VideoDao
import com.videorelay.app.data.db.VideoEntity
import com.videorelay.app.data.nostr.*
import com.videorelay.app.domain.model.Video
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val relayPool: RelayPool,
    private val videoDao: VideoDao,
    private val relayRepository: RelayRepository,
) {
    companion object {
        private const val CACHE_MAX_AGE_MS = 30 * 60 * 1000L // 30 min
    }

    /**
     * Fetch videos from relays with caching.
     * Uses fast relays first for quick initial load, then backfills from all relays.
     */
    suspend fun fetchVideos(
        limit: Int = 200,
        authors: List<String>? = null,
        hashtag: String? = null,
        since: Long? = null,
        until: Long? = null,
    ): List<Video> {
        val relays = relayRepository.getActiveRelays()
        if (relays.isEmpty()) return emptyList()

        // Empty authors list means "no one to filter by" → return empty
        if (authors != null && authors.isEmpty()) return emptyList()

        val filter = NostrFilter(
            kinds = NostrConstants.ALL_VIDEO_KINDS,
            authors = authors?.takeIf { it.isNotEmpty() },
            limit = limit,
            since = since,
            until = until,
            tags = if (hashtag != null) mapOf("#t" to listOf(hashtag.lowercase())) else emptyMap(),
        )

        // Query fast relays first (2s timeout) for quick results
        val fastRelays = relays.filter { it in NostrConstants.FAST_RELAYS }
        val fastEvents = if (fastRelays.isNotEmpty()) {
            relayPool.query(fastRelays, filter, timeoutMs = 2500)
        } else emptyList()

        // Then query remaining relays
        val slowRelays = relays.filter { it !in NostrConstants.FAST_RELAYS }
        val slowEvents = if (slowRelays.isNotEmpty()) {
            relayPool.query(slowRelays, filter, timeoutMs = 4000)
        } else emptyList()

        val allEvents = (fastEvents + slowEvents)
        val videos = allEvents.mapNotNull { NIP71Parser.parse(it) }
            .distinctBy { it.id }

        // Cache to Room
        videoDao.insertAll(videos.map { VideoEntity.from(it) })

        return videos
    }

    /**
     * Get cached videos (for instant display before network loads).
     */
    suspend fun getCachedVideos(limit: Int = 200): List<Video> {
        return videoDao.getRecentVideos(limit).map { it.toDomain() }
    }

    suspend fun getCachedShorts(limit: Int = 100): List<Video> {
        return videoDao.getShorts(limit).map { it.toDomain() }
    }

    suspend fun getMostZapped(limit: Int = 50): List<Video> {
        return videoDao.getMostZapped(limit).map { it.toDomain() }
    }

    /**
     * Fetch zap counts for a list of videos.
     */
    suspend fun fetchZapCounts(videos: List<Video>): Map<String, Int> {
        val relays = relayRepository.getActiveRelays()
            .filter { "nostr.band" in it || "primal" in it || "nostr.wine" in it }
            .ifEmpty { relayRepository.getActiveRelays().take(3) }

        val zapCounts = mutableMapOf<String, Int>()

        videos.chunked(50).forEach { chunk ->
            val filter = NostrFilter(
                kinds = listOf(NostrConstants.ZAP_RECEIPT_KIND),
                tags = mapOf("#e" to chunk.map { it.id }),
                limit = 500,
            )

            val zapEvents = relayPool.query(relays, filter)
            for (event in zapEvents) {
                val eTag = event.getTag("e") ?: continue
                var amount = 1
                val descTag = event.getTag("description")
                if (descTag != null) {
                    try {
                        val desc = kotlinx.serialization.json.Json.parseToJsonElement(descTag)
                        // Extract amount from zap request description
                        val tags = desc.jsonObject["tags"]?.jsonArray
                        val amountTag = tags?.firstOrNull { arr ->
                            arr.jsonArray[0].jsonPrimitive.content == "amount"
                        }
                        amountTag?.let {
                            amount = (it.jsonArray[1].jsonPrimitive.content.toLong() / 1000).toInt()
                        }
                    } catch (_: Exception) {}
                }
                zapCounts[eTag] = (zapCounts[eTag] ?: 0) + amount
            }
        }

        return zapCounts
    }

    suspend fun clearOldCache() {
        videoDao.deleteOlderThan(System.currentTimeMillis() - CACHE_MAX_AGE_MS)
    }
}

// Extension imports for JSON parsing
private val kotlinx.serialization.json.JsonElement.jsonObject
    get() = this as kotlinx.serialization.json.JsonObject
private val kotlinx.serialization.json.JsonElement.jsonArray
    get() = this as kotlinx.serialization.json.JsonArray
private val kotlinx.serialization.json.JsonElement.jsonPrimitive
    get() = this as kotlinx.serialization.json.JsonPrimitive
