package com.videorelay.app.data.nostr

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.videorelay.app.domain.model.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.discoveryStore by preferencesDataStore(name = "discovery_cache")

data class EngagementScore(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val zapAmount: Long = 0,
    val zapCount: Int = 0,
    val score: Double = 0.0,
    val ratioed: Boolean = false,
)

enum class SortMode { Trending, MostZapped }

@Singleton
class VideoDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val relayPool: RelayPool,
) {
    companion object {
        private const val TAG = "VideoDiscovery"
        private val KEY_CACHE = stringPreferencesKey("engagement_cache")
        private val KEY_CACHE_TS = longPreferencesKey("engagement_cache_ts")
        private const val CACHE_TTL_MS = 15 * 60 * 1000L

        // Aggregator relays — match web app's DISCOVERY_RELAYS
        val DISCOVERY_RELAYS = listOf(
            "wss://relay.nostr.band",
            "wss://relay.primal.net",
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://relay.snort.social",
            "wss://nostr.wine",
            "wss://offchain.pub",
        )

        // Popular tags for content discovery (mirrors web app)
        val POPULAR_TAGS = listOf(
            "bitcoin", "nostr", "video", "podcast", "music",
            "tutorial", "news", "tech", "privacy", "lightning",
        )
    }

    private val engagementCache = mutableMapOf<String, EngagementScore>()
    private var cacheTimestamp = 0L

    /**
     * MOST ZAPPED approach: fetch zap receipts first, then get the videos they reference.
     * This guarantees we only return videos that actually have zaps.
     * Much better than fetching videos and hoping they have zaps.
     */
    suspend fun fetchMostZappedVideos(
        timePeriodSeconds: Long = Long.MAX_VALUE,
    ): List<Video> = coroutineScope {
        val now = System.currentTimeMillis() / 1000
        val since = if (timePeriodSeconds == Long.MAX_VALUE) null else now - timePeriodSeconds

        Log.d(TAG, "Fetching zap receipts to find most-zapped videos...")

        // Step 1: Fetch recent zap receipts (kind 9735) from aggregator relays
        val zapRelays = listOf("wss://relay.nostr.band", "wss://relay.primal.net", "wss://nostr.wine")
        val zapEvents = try {
            relayPool.query(
                zapRelays,
                NostrFilter(
                    kinds = listOf(NostrConstants.ZAP_RECEIPT_KIND),
                    since = since,
                    limit = 1000,
                ),
                timeoutMs = 8000,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Zap receipt fetch failed: ${e.message}")
            emptyList()
        }

        Log.d(TAG, "Got ${zapEvents.size} zap receipts")

        if (zapEvents.isEmpty()) return@coroutineScope emptyList()

        // Step 2: Extract video IDs from zap receipts + compute sats per video
        data class ZapInfo(var sats: Long, var count: Int)
        val videoZaps = mutableMapOf<String, ZapInfo>()

        for (zap in zapEvents) {
            val videoId = zap.getTag("e") ?: continue
            var sats = 0L
            try {
                val desc = zap.getTag("description")
                if (desc != null) {
                    val descJson = kotlinx.serialization.json.Json.parseToJsonElement(desc)
                        .jsonObject
                    val amountTag = descJson["tags"]?.jsonArray?.firstOrNull {
                        it.jsonArray.firstOrNull()?.jsonPrimitive?.content == "amount"
                    }
                    sats = amountTag?.jsonArray?.get(1)?.jsonPrimitive?.content?.toLong()?.div(1000) ?: 0L
                }
            } catch (_: Exception) {}

            val info = videoZaps.getOrPut(videoId) { ZapInfo(0, 0) }
            info.sats += sats
            info.count++
        }

        // Step 3: Sort by sats (or zap count as fallback), take top 200
        val topVideoIds = videoZaps.entries
            .sortedByDescending { (_, info) -> info.sats * 1000 + info.count }
            .take(200)
            .map { it.key }

        Log.d(TAG, "Top ${topVideoIds.size} video IDs found with zaps")

        if (topVideoIds.isEmpty()) return@coroutineScope emptyList()

        // Step 4: Fetch the actual video events by ID
        val videoEvents = try {
            relayPool.query(
                DISCOVERY_RELAYS.take(4),
                NostrFilter(
                    ids = topVideoIds.take(100), // relay limit
                    limit = 100,
                ),
                timeoutMs = 6000,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Video fetch by ID failed: ${e.message}")
            emptyList()
        }

        // Step 5: Parse, attach zap data, and sort
        val videos = videoEvents.mapNotNull { NIP71Parser.parse(it) }
            .distinctBy { it.id }
            .map { video ->
                val zapInfo = videoZaps[video.id]
                if (zapInfo != null) video.copy(zapCount = zapInfo.sats.toInt()) else video
            }
            .sortedByDescending { it.zapCount }

        Log.d(TAG, "Returning ${videos.size} most-zapped videos")
        videos
    }

    /**
     * Fetch a broad set of videos using multiple strategies (mirrors web's discoverPopularVideos).
     * Uses time-windowed queries + tag-based queries + big unlimited query.
     */
    suspend fun fetchBroadVideoSet(
        limit: Int = 500,
        timePeriodSeconds: Long = Long.MAX_VALUE,
    ): List<Video> = coroutineScope {
        val seen = mutableSetOf<String>()
        val allVideos = mutableListOf<Video>()

        val now = System.currentTimeMillis() / 1000
        val day = 86_400L
        val discoveryRelays = DISCOVERY_RELAYS.take(4)

        // Strategy 1: Big query with no time filter (aggregators return recent popular content)
        val bigQueryJob = async {
            try {
                val filter = NostrFilter(
                    kinds = NostrConstants.ALL_VIDEO_KINDS,
                    limit = minOf(limit, 300),
                    since = if (timePeriodSeconds == Long.MAX_VALUE) null else now - timePeriodSeconds,
                )
                relayPool.query(discoveryRelays, filter, timeoutMs = 6000)
            } catch (_: Exception) { emptyList() }
        }

        // Strategy 2: Time-windowed queries to get content from all eras
        val timeWindowJobs = if (timePeriodSeconds == Long.MAX_VALUE) {
            listOf(
                async {
                    try {
                        relayPool.query(
                            listOf("wss://relay.nostr.band"),
                            NostrFilter(kinds = NostrConstants.ALL_VIDEO_KINDS, since = now - 7 * day, limit = 100),
                            timeoutMs = 5000,
                        )
                    } catch (_: Exception) { emptyList() }
                },
                async {
                    try {
                        relayPool.query(
                            listOf("wss://relay.nostr.band"),
                            NostrFilter(kinds = NostrConstants.ALL_VIDEO_KINDS, since = now - 30 * day, until = now - 7 * day, limit = 100),
                            timeoutMs = 5000,
                        )
                    } catch (_: Exception) { emptyList() }
                },
                async {
                    try {
                        relayPool.query(
                            listOf("wss://relay.nostr.band"),
                            NostrFilter(kinds = NostrConstants.ALL_VIDEO_KINDS, since = now - 90 * day, until = now - 30 * day, limit = 100),
                            timeoutMs = 5000,
                        )
                    } catch (_: Exception) { emptyList() }
                },
                async {
                    try {
                        relayPool.query(
                            listOf("wss://relay.nostr.band"),
                            NostrFilter(kinds = NostrConstants.ALL_VIDEO_KINDS, since = now - 365 * day, until = now - 90 * day, limit = 100),
                            timeoutMs = 5000,
                        )
                    } catch (_: Exception) { emptyList() }
                },
            )
        } else emptyList()

        // Strategy 3: Tag-based queries for categorized content
        val tagJobs = POPULAR_TAGS.take(5).map { tag ->
            async {
                try {
                    val since = if (timePeriodSeconds == Long.MAX_VALUE) null else now - timePeriodSeconds
                    relayPool.query(
                        listOf("wss://relay.nostr.band", "wss://relay.primal.net"),
                        NostrFilter(kinds = NostrConstants.ALL_VIDEO_KINDS, tags = mapOf("#t" to listOf(tag)), since = since, limit = 50),
                        timeoutMs = 4000,
                    )
                } catch (_: Exception) { emptyList() }
            }
        }

        // Collect all results
        val allJobs = listOf(bigQueryJob) + timeWindowJobs + tagJobs
        val results = allJobs.awaitAll()

        for (eventList in results) {
            for (event in eventList) {
                if (seen.add(event.id)) {
                    val video = NIP71Parser.parse(event)
                    if (video != null) allVideos.add(video)
                }
            }
        }

        Log.d(TAG, "Fetched ${allVideos.size} videos across ${allJobs.size} strategies")
        allVideos
    }

    /**
     * Fetch engagement scores (likes, comments, reposts, zaps) for video IDs.
     * Queries kind 7 (reactions), 6/16 (reposts), 1111 (comments), 9735 (zaps).
     */
    suspend fun fetchEngagementScores(videoIds: List<String>): Map<String, EngagementScore> {
        if (videoIds.isEmpty()) return emptyMap()

        if (engagementCache.isEmpty()) loadCache()

        val now = System.currentTimeMillis()
        val cacheValid = (now - cacheTimestamp) < CACHE_TTL_MS
        val scores = mutableMapOf<String, EngagementScore>()
        val uncached = mutableListOf<String>()

        for (id in videoIds) {
            val cached = if (cacheValid) engagementCache[id] else null
            scores[id] = cached ?: EngagementScore()
            if (cached == null) uncached.add(id)
        }

        if (uncached.isEmpty()) return scores

        val engagementRelays = DISCOVERY_RELAYS.take(4)

        uncached.chunked(50).forEach { chunk ->
            try {
                coroutineScope {
                    val reactionsJob = async {
                        try {
                            relayPool.query(
                                engagementRelays,
                                NostrFilter(kinds = listOf(7, 6, 16), tags = mapOf("#e" to chunk), limit = 2000),
                                timeoutMs = 6000,
                            )
                        } catch (_: Exception) { emptyList() }
                    }
                    val commentsJob = async {
                        try {
                            relayPool.query(
                                engagementRelays,
                                NostrFilter(kinds = listOf(1111), tags = mapOf("#e" to chunk), limit = 1000),
                                timeoutMs = 6000,
                            )
                        } catch (_: Exception) { emptyList() }
                    }
                    val zapsJob = async {
                        try {
                            relayPool.query(
                                engagementRelays,
                                NostrFilter(kinds = listOf(NostrConstants.ZAP_RECEIPT_KIND), tags = mapOf("#e" to chunk), limit = 2000),
                                timeoutMs = 6000,
                            )
                        } catch (_: Exception) { emptyList() }
                    }

                    val reactions = reactionsJob.await()
                    val comments = commentsJob.await()
                    val zaps = zapsJob.await()

                    for (event in reactions) {
                        val videoId = event.getTag("e") ?: continue
                        val entry = scores[videoId] ?: continue
                        scores[videoId] = when (event.kind) {
                            7 -> if (event.content == "-") entry.copy(dislikes = entry.dislikes + 1)
                                 else entry.copy(likes = entry.likes + 1)
                            6, 16 -> entry.copy(reposts = entry.reposts + 1)
                            else -> entry
                        }
                    }

                    for (event in comments) {
                        val videoId = event.getTag("e") ?: continue
                        val entry = scores[videoId] ?: continue
                        scores[videoId] = entry.copy(comments = entry.comments + 1)
                    }

                    for (event in zaps) {
                        val videoId = event.getTag("e") ?: continue
                        val entry = scores[videoId] ?: continue
                        var zapSats = 0L
                        try {
                            val desc = event.getTag("description")
                            if (desc != null) {
                                val descJson = Json.parseToJsonElement(desc).jsonObject
                                val amountTag = descJson["tags"]?.jsonArray?.firstOrNull {
                                    it.jsonArray.firstOrNull()?.jsonPrimitive?.content == "amount"
                                }
                                zapSats = amountTag?.jsonArray?.get(1)?.jsonPrimitive?.content?.toLong()?.div(1000) ?: 0L
                            }
                        } catch (_: Exception) {}
                        scores[videoId] = entry.copy(
                            zapCount = entry.zapCount + 1,
                            zapAmount = entry.zapAmount + zapSats,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Engagement fetch error: ${e.message}")
            }
        }

        // Compute composite scores and cache
        for ((id, entry) in scores) {
            val ratioed = entry.dislikes > entry.likes * 2 && entry.dislikes > 3
            val score = (entry.likes * 0.5)
                .minus(entry.dislikes * 2.0)
                .plus((entry.zapAmount / 1000.0) * 5.0)
                .plus(entry.comments * 0.3)
                .plus(entry.reposts * 1.0)
            scores[id] = entry.copy(ratioed = ratioed, score = score)
            engagementCache[id] = scores[id]!!
        }

        cacheTimestamp = System.currentTimeMillis()
        saveCache()
        return scores
    }

    /**
     * Sort videos by mode using full engagement scoring.
     * For MostZapped with All Time: fetches videos from all time periods first.
     */
    suspend fun sortByEngagement(
        videos: List<Video>,
        mode: SortMode,
        timePeriodSeconds: Long = Long.MAX_VALUE,
    ): List<Video> {
        if (videos.isEmpty()) return emptyList()

        val now = System.currentTimeMillis() / 1000
        val filtered = if (timePeriodSeconds == Long.MAX_VALUE) videos
                       else videos.filter { it.publishedAt >= now - timePeriodSeconds }

        if (filtered.isEmpty()) return emptyList()

        val scores = fetchEngagementScores(filtered.map { it.id })
        val clean = filtered.filter { scores[it.id]?.ratioed != true }

        return when (mode) {
            SortMode.MostZapped -> {
                val withZaps = clean.filter {
                    (scores[it.id]?.zapAmount ?: 0L) > 0 || (scores[it.id]?.zapCount ?: 0) > 0
                }
                val sorted = withZaps.sortedByDescending { v ->
                    val e = scores[v.id]
                    (e?.zapAmount ?: 0L) * 1000 + (e?.zapCount ?: 0)
                }
                // If no zapped videos found, show by score instead of empty
                sorted.ifEmpty { clean.sortedByDescending { v -> scores[v.id]?.score ?: 0.0 } }
            }
            SortMode.Trending -> {
                clean.sortedByDescending { v ->
                    val e = scores[v.id]
                    val score = e?.score ?: 0.0
                    val ageHours = ((now - v.publishedAt) / 3600.0).coerceAtLeast(1.0)
                    val decay = (168.0 - ageHours.coerceAtMost(168.0)) / 168.0
                    score + decay * 10.0
                }
            }
        }
    }

    private suspend fun loadCache() {
        try {
            val prefs = context.discoveryStore.data.first()
            cacheTimestamp = prefs[KEY_CACHE_TS] ?: 0L
            val raw = prefs[KEY_CACHE] ?: return
            val data = Json.parseToJsonElement(raw).jsonObject
            for ((id, obj) in data) {
                val e = obj.jsonObject
                engagementCache[id] = EngagementScore(
                    likes = e["l"]?.jsonPrimitive?.intOrNull ?: 0,
                    dislikes = e["d"]?.jsonPrimitive?.intOrNull ?: 0,
                    comments = e["c"]?.jsonPrimitive?.intOrNull ?: 0,
                    reposts = e["r"]?.jsonPrimitive?.intOrNull ?: 0,
                    zapAmount = e["za"]?.jsonPrimitive?.longOrNull ?: 0L,
                    zapCount = e["zc"]?.jsonPrimitive?.intOrNull ?: 0,
                    score = e["s"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    ratioed = e["rt"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            }
        } catch (_: Exception) {}
    }

    private suspend fun saveCache() {
        try {
            val data = buildJsonObject {
                engagementCache.entries.take(500).forEach { (id, e) ->
                    put(id, buildJsonObject {
                        put("l", e.likes); put("d", e.dislikes)
                        put("c", e.comments); put("r", e.reposts)
                        put("za", e.zapAmount); put("zc", e.zapCount)
                        put("s", e.score); put("rt", e.ratioed)
                    })
                }
            }
            context.discoveryStore.edit {
                it[KEY_CACHE] = data.toString()
                it[KEY_CACHE_TS] = cacheTimestamp
            }
        } catch (_: Exception) {}
    }
}
