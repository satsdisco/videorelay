package com.videorelay.app.ui.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.VideoRepository
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val seenIds = mutableSetOf<String>()
    private var lastTimestamp: Long? = null

    init {
        loadShorts()
    }

    fun setCurrentIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
        // Auto-load more when near the end
        val shorts = _uiState.value.shorts
        if (index >= shorts.size - 3 && shorts.isNotEmpty()) {
            loadMore()
        }
    }

    fun refresh() {
        seenIds.clear()
        lastTimestamp = null
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                val shorts = fetchAndDedupe()
                val pubkeys = shorts.map { it.pubkey }.distinct()
                val profiles = profileRepository.getProfiles(pubkeys)

                _uiState.value = ShortsUiState(
                    shorts = shorts,
                    profiles = profiles,
                    isLoading = false,
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun loadShorts() {
        viewModelScope.launch {
            try {
                val shorts = fetchAndDedupe()
                val pubkeys = shorts.map { it.pubkey }.distinct()
                val profiles = profileRepository.getProfiles(pubkeys)

                _uiState.value = ShortsUiState(
                    shorts = shorts,
                    profiles = profiles,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = ShortsUiState(isLoading = false)
            }
        }
    }

    private fun loadMore() {
        val current = _uiState.value
        if (current.isRefreshing) return

        viewModelScope.launch {
            try {
                val moreShorts = fetchAndDedupe()
                if (moreShorts.isEmpty()) return@launch

                val allShorts = current.shorts + moreShorts
                val newPubkeys = moreShorts.map { it.pubkey }.distinct()
                    .filter { it !in current.profiles }
                val newProfiles = if (newPubkeys.isNotEmpty()) {
                    profileRepository.getProfiles(newPubkeys)
                } else emptyMap()

                _uiState.value = current.copy(
                    shorts = allShorts,
                    profiles = current.profiles + newProfiles,
                )
            } catch (_: Exception) {}
        }
    }

    /**
     * Fetch shorts with diversity:
     * - Limit per author (max 2 per fetch) to avoid one person dominating
     * - Shuffle result for variety
     * - Track seen IDs to avoid repeats across loads
     * - Falls back to short-ish videos (< 3 min) if not enough true shorts
     */
    private suspend fun fetchAndDedupe(): List<Video> {
        // Vary the query to get different content each time
        val now = System.currentTimeMillis() / 1000
        val randomOffset = (0..7).random() * 86400L // random day offset up to a week
        val allVideos = videoRepository.fetchVideos(
            limit = 200,
            until = lastTimestamp ?: (now - randomOffset),
        )

        // Primary: videos marked as shorts
        var shorts = allVideos
            .filter { it.isShort && it.id !in seenIds }

        // Fallback: if we have very few shorts, include videos under 3 minutes
        if (shorts.size < 5) {
            val shortish = allVideos.filter {
                !it.isShort && it.id !in seenIds &&
                    it.durationSeconds in 1..180
            }
            shorts = shorts + shortish
        }

        // Limit per author — max 2 per load to ensure diversity
        val diverse = shorts
            .groupBy { it.pubkey }
            .flatMap { (_, videos) ->
                videos.shuffled().take(2)
            }
            .shuffled()

        // Track what we've shown
        diverse.forEach { seenIds.add(it.id) }

        // Update pagination cursor
        if (allVideos.isNotEmpty()) {
            lastTimestamp = allVideos.minOf { it.publishedAt }
        }

        return diverse
    }
}
