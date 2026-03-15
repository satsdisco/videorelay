package com.videorelay.app.ui.shorts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.*
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.RelayRepository
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShortsUiState(
    val shorts: List<Video> = emptyList(),
    val profiles: Map<String, Profile> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentIndex: Int = 0,
)

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "ShortsViewModel"

        // Short-specific hashtags matching the web app exactly
        private val SHORT_HASHTAGS = listOf("shorts", "short", "clip", "reel", "vertical", "reels", "clips")

        // Fast relays for content discovery (matching web's DEFAULT_RELAYS.slice(0,5))
        private val SHORTS_RELAYS = listOf(
            "wss://relay.nostr.band",
            "wss://relay.primal.net",
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://video.nostr.build",
        )
    }

    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val seenIds = mutableSetOf<String>()
    private var loadedPage = 0

    init {
        loadShorts()
    }

    fun setCurrentIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
        // Auto-load more when near end
        if (index >= _uiState.value.shorts.size - 3) {
            loadMoreShorts()
        }
    }

    fun refresh() {
        seenIds.clear()
        loadedPage = 0
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val shorts = fetchShorts()
            val profiles = profileRepository.getProfiles(shorts.map { it.pubkey }.distinct())
            _uiState.value = ShortsUiState(
                shorts = shorts,
                profiles = profiles,
                isLoading = false,
                isRefreshing = false,
            )
        }
    }

    private fun loadShorts() {
        viewModelScope.launch {
            val shorts = fetchShorts()
            val profiles = profileRepository.getProfiles(shorts.map { it.pubkey }.distinct())
            _uiState.value = ShortsUiState(
                shorts = shorts,
                profiles = profiles,
                isLoading = false,
            )
        }
    }

    private fun loadMoreShorts() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            val more = fetchShorts()
            if (more.isEmpty()) return@launch
            val current = _uiState.value
            val allShorts = current.shorts + more
            val newPubkeys = more.map { it.pubkey }.distinct().filter { it !in current.profiles }
            val newProfiles = if (newPubkeys.isNotEmpty()) {
                profileRepository.getProfiles(newPubkeys)
            } else emptyMap()
            _uiState.value = current.copy(
                shorts = allShorts,
                profiles = current.profiles + newProfiles,
            )
        }
    }

    /**
     * Fetch shorts using the same dual-query strategy as web app:
     * 1. Big query with all video kinds (limit 300)
     * 2. Tag-filtered query for short-specific hashtags (limit 100)
     * Both run in parallel, deduplicated, shuffled with per-author limit.
     */
    private suspend fun fetchShorts(): List<Video> = coroutineScope {
        val now = System.currentTimeMillis() / 1000
        // Random time offset for variety (0-14 days back)
        val timeOffset = (loadedPage * 7 * 86400L).coerceAtMost(30 * 86400L)
        val until = if (timeOffset > 0) now - timeOffset else null
        loadedPage++

        try {
            // Query 1: All video kinds, big limit (mirrors web's DEFAULT_RELAYS.slice(0,5) query)
            val bigQuery = async {
                try {
                    relayPool.query(
                        SHORTS_RELAYS,
                        NostrFilter(
                            kinds = NostrConstants.ALL_VIDEO_KINDS,
                            limit = 300,
                            until = until,
                        ),
                        timeoutMs = 6000,
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Big query failed: ${e.message}")
                    emptyList()
                }
            }

            // Query 2: Short-specific hashtags (mirrors web's #t filter query)
            val tagQuery = async {
                try {
                    relayPool.query(
                        SHORTS_RELAYS.take(4),
                        NostrFilter(
                            kinds = NostrConstants.ALL_VIDEO_KINDS,
                            tags = mapOf("#t" to SHORT_HASHTAGS),
                            limit = 100,
                            until = until,
                        ),
                        timeoutMs = 5000,
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Tag query failed: ${e.message}")
                    emptyList()
                }
            }

            // Query 3: Kind 22 specifically (short video kind)
            val kindQuery = async {
                try {
                    relayPool.query(
                        SHORTS_RELAYS.take(3),
                        NostrFilter(
                            kinds = listOf(NostrConstants.SHORT_VIDEO_KIND, NostrConstants.ADDRESSABLE_SHORT_KIND),
                            limit = 100,
                            until = until,
                        ),
                        timeoutMs = 5000,
                    )
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val allEvents = (bigQuery.await() + tagQuery.await() + kindQuery.await())

            // Parse and filter
            val parsed = allEvents
                .mapNotNull { NIP71Parser.parse(it) }
                .filter { it.id !in seenIds }
                .distinctBy { it.id }

            // Filter to actual short-form content (matching web logic):
            // isShort flag OR duration <= 60s (web also checks isVertical which we can't probe)
            val filtered = parsed.filter { v ->
                v.isShort || (v.durationSeconds in 1..60)
            }

            // If very few results, be more lenient — include anything under 3 min
            val candidates = if (filtered.size < 10) {
                parsed.filter { v -> v.isShort || v.durationSeconds in 1..180 }
            } else filtered

            // Max 2 per author for diversity, shuffled
            val diverse = candidates
                .groupBy { it.pubkey }
                .flatMap { (_, vids) -> vids.shuffled().take(2) }
                .shuffled()

            diverse.forEach { seenIds.add(it.id) }
            Log.d(TAG, "Fetched ${diverse.size} shorts from ${allEvents.size} events")
            diverse
        } catch (e: Exception) {
            Log.e(TAG, "Shorts fetch failed", e)
            emptyList()
        }
    }
}
