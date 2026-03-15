package com.videorelay.app.data.nostr

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
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
    val zapAmount: Long = 0,  // in sats
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
        private const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 min

        // Aggregator relays with broadest content index
        val DISCOVERY_RELAYS = listOf(
            "wss://relay.nostr.band",
            "wss://relay.primal.net",
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://relay.snort.social",
            "wss://nostr.wine",
            "wss://offchain.pub",
        )
    }

    private val engagementCache = mutableMapOf<String, EngagementScore>()
    private var cacheTimestamp = 0L

    /**
     * Fetch engagement scores (likes, dislikes, comments, reposts, zaps)
     * for a list of video IDs. Mirrors web app's fetchEngagementScores().
     */
    suspend fun fetchEngagementScores(videoIds: List<String>): Map<String, EngagementScore> {
        if (videoIds.isEmpty()) return emptyMap()

        // Load cache
        if (engagementCache.isEmpty()) loadCache()

        val now = System.currentTimeMillis()
        val cacheValid = (now - cacheTimestamp) < CACHE_TTL_MS

        val scores = mutableMapOf<String, EngagementScore>()
        val uncached = mutableListOf<String>()

        for (id in videoIds) {
            val cached = if (cacheValid) engagementCache[id] else null
            if (cached != null) {
                scores[id] = cached
            } else {
                scores[id] = EngagementScore()
                uncached.add(id)
            }
        }

        if (uncached.isEmpty()) return scores

        // Query engagement relays in chunks of 50
        val engagementRelays = DISCOVERY_RELAYS.take(6)

        uncached.chunked(50).forEach { chunk ->
            try {
                coroutineScope {
                    val reactionsJob = async {
                        relayPool.query(
                            engagementRelays,
                            NostrFilter(kinds = listOf(7, 6, 16), tags = mapOf("#e" to chunk), limit = 2000),
                            timeoutMs = 5000,
                        )
                    }
                    val commentsJob = async {
                        relayPool.query(
                            engagementRelays,
                            NostrFilter(kinds = listOf(1111, 1), tags = mapOf("#e" to chunk), limit = 1000),
                            timeoutMs = 5000,
                        )
                    }
                    val zapsJob = async {
                        relayPool.query(
                            engagementRelays,
                            NostrFilter(kinds = listOf(NostrConstants.ZAP_RECEIPT_KIND), tags = mapOf("#e" to chunk), limit = 2000),
                            timeoutMs = 5000,
                        )
                    }

                    val reactions = reactionsJob.await()
                    val comments = commentsJob.await()
                    val zaps = zapsJob.await()

                    // Count reactions
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

                    // Count comments
                    for (event in comments) {
                        val videoId = event.getTag("e") ?: continue
                        val entry = scores[videoId] ?: continue
                        scores[videoId] = entry.copy(comments = entry.comments + 1)
                    }

                    // Count zaps
                    for (event in zaps) {
                        val videoId = event.getTag("e") ?: continue
                        val entry = scores[videoId] ?: continue
                        var zapAmount = 0L
                        val descTag = event.getTag("description")
                        if (descTag != null) {
                            try {
                                val desc = Json.parseToJsonElement(descTag).jsonObject
                                val tags = desc["tags"]?.jsonArray
                                val amountTag = tags?.firstOrNull {
                                    it.jsonArray.firstOrNull()?.jsonPrimitive?.content == "amount"
                                }
                                amountTag?.let {
                                    zapAmount = it.jsonArray[1].jsonPrimitive.content.toLong() / 1000
                                }
                            } catch (_: Exception) {}
                        }
                        scores[videoId] = entry.copy(
                            zapCount = entry.zapCount + 1,
                            zapAmount = entry.zapAmount + zapAmount,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Engagement fetch error: ${e.message}")
            }
        }

        // Compute composite scores
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
     * Sort videos by trending or most zapped using full engagement scores.
     * Mirrors web app's discoverPopularVideos().
     */
    suspend fun sortByEngagement(
        videos: List<Video>,
        mode: SortMode,
        timePeriodSeconds: Long = Long.MAX_VALUE,
    ): List<Video> {
        if (videos.isEmpty()) return emptyList()

        val now = System.currentTimeMillis() / 1000

        // Apply time filter
        val filtered = if (timePeriodSeconds == Long.MAX_VALUE) videos
        else videos.filter { it.publishedAt >= now - timePeriodSeconds }

        if (filtered.isEmpty()) return emptyList()

        val scores = fetchEngagementScores(filtered.map { it.id })

        // Filter ratioed content
        val clean = filtered.filter { scores[it.id]?.ratioed != true }

        return when (mode) {
            SortMode.MostZapped -> {
                clean
                    .filter { (scores[it.id]?.zapAmount ?: 0) > 0 || (scores[it.id]?.zapCount ?: 0) > 0 }
                    .sortedByDescending { v ->
                        val e = scores[v.id]
                        (e?.zapAmount ?: 0L) * 1000 + (e?.zapCount ?: 0)
                    }
                    .ifEmpty { clean.sortedByDescending { v -> scores[v.id]?.zapCount ?: 0 } }
            }
            SortMode.Trending -> {
                clean.sortedByDescending { v ->
                    val e = scores[v.id]
                    val score = e?.score ?: 0.0
                    // Time decay: boost content from last week
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
            val raw = prefs[KEY_CACHE] ?: return
            val json = Json.parseToJsonElement(raw).jsonObject
            val ts = json["ts"]?.jsonPrimitive?.longOrNull ?: 0L
            val data = json["data"]?.jsonObject ?: return
            cacheTimestamp = ts
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
                put("ts", cacheTimestamp)
                put("data", buildJsonObject {
                    engagementCache.entries.take(500).forEach { (id, e) ->
                        put(id, buildJsonObject {
                            put("l", e.likes); put("d", e.dislikes)
                            put("c", e.comments); put("r", e.reposts)
                            put("za", e.zapAmount); put("zc", e.zapCount)
                            put("s", e.score); put("rt", e.ratioed)
                        })
                    }
                })
            }
            context.discoveryStore.edit { it[KEY_CACHE] = data.toString() }
        } catch (_: Exception) {}
    }
}
