package com.videorelay.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.SortMode
import com.videorelay.app.data.nostr.VideoDiscovery
import com.videorelay.app.data.repository.CuratedRepository
import com.videorelay.app.data.repository.FollowRepository
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.VideoRepository
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class FeedTab { Home, Trending, MostZapped, Following }

enum class TimePeriod(val label: String, val seconds: Long) {
    Today("Today", 86_400L),
    Week("This Week", 604_800L),
    Month("This Month", 2_592_000L),
    All("All Time", Long.MAX_VALUE),
}

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val profiles: Map<String, Profile> = emptyMap(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedTab: FeedTab = FeedTab.Home,
    val selectedTag: String? = null,
    val timePeriod: TimePeriod = TimePeriod.All,
    val hasMore: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository,
    private val followRepository: FollowRepository,
    private val curatedRepository: CuratedRepository,
    private val videoDiscovery: VideoDiscovery,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allVideos: List<Video> = emptyList()
    private var allProfiles: Map<String, Profile> = emptyMap()

    init {
        loadVideos()
    }

    fun selectTab(tab: FeedTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.value = _uiState.value.copy(selectedTab = tab, selectedTag = null)
        loadVideos()
    }

    fun selectTag(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        loadVideos()
    }

    fun selectTimePeriod(period: TimePeriod) {
        if (period == _uiState.value.timePeriod) return
        _uiState.value = _uiState.value.copy(timePeriod = period)
        loadVideos()
    }

    fun refresh() {
        loadVideos()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || allVideos.isEmpty()) return
        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val oldest = allVideos.minOf { it.publishedAt }
                val moreVideos = videoRepository.fetchVideos(
                    limit = 100,
                    hashtag = state.selectedTag,
                    until = oldest - 1,
                )

                allVideos = (allVideos + moreVideos).distinctBy { it.id }
                val newPubkeys = moreVideos.map { it.pubkey }.distinct().filter { it !in allProfiles }
                val newProfiles = profileRepository.getProfiles(newPubkeys)
                allProfiles = allProfiles + newProfiles

                _uiState.value = state.copy(
                    videos = allVideos,
                    profiles = allProfiles,
                    isLoadingMore = false,
                    hasMore = moreVideos.size >= 10,
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(isLoadingMore = false)
            }
        }
    }

    private fun loadVideos() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null, videos = emptyList())

        viewModelScope.launch {
            try {
                when (state.selectedTab) {
                    FeedTab.Home -> loadHomeFeed(state)
                    FeedTab.Trending -> loadTrendingFeed(state)
                    FeedTab.MostZapped -> loadMostZappedFeed(state)
                    FeedTab.Following -> loadFollowingFeed(state)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Load failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load videos",
                )
            }
        }
    }

    /**
     * Home feed: curated creators first (NIP-51 list), then recent from all relays.
     * Mirrors web app's home view with curated priority.
     */
    private suspend fun loadHomeFeed(state: HomeUiState) {
        // Fetch curated pubkeys in background
        val curatedPubkeys = try { curatedRepository.getCuratedPubkeys() } catch (_: Exception) { emptyList() }

        // Fetch recent videos (broad)
        val recent = videoRepository.fetchVideos(hashtag = state.selectedTag)
            .sortedByDescending { it.publishedAt }

        // Boost curated creators to top
        val curated = if (curatedPubkeys.isNotEmpty()) {
            recent.filter { it.pubkey in curatedPubkeys }
        } else emptyList()

        val rest = recent.filter { it.pubkey !in curatedPubkeys }
        allVideos = (curated + rest).distinctBy { it.id }

        val pubkeys = allVideos.map { it.pubkey }.distinct()
        allProfiles = profileRepository.getProfiles(pubkeys)

        _uiState.value = _uiState.value.copy(
            videos = allVideos,
            profiles = allProfiles,
            isLoading = false,
            hasMore = allVideos.size >= 50,
        )
    }

    /**
     * Trending: full engagement score (likes + dislikes + comments + reposts + zaps + time decay).
     * Mirrors web app's discoverPopularVideos with sortBy=trending.
     */
    private suspend fun loadTrendingFeed(state: HomeUiState) {
        val raw = videoRepository.fetchVideos(hashtag = state.selectedTag, limit = 300)
        allVideos = raw.distinctBy { it.id }
        val pubkeys = allVideos.map { it.pubkey }.distinct()
        allProfiles = profileRepository.getProfiles(pubkeys)

        // Show sorted-by-recent while engagement loads
        _uiState.value = _uiState.value.copy(
            videos = allVideos.sortedByDescending { it.publishedAt },
            profiles = allProfiles,
            isLoading = false,
        )

        // Fetch engagement scores + re-sort in background
        try {
            val sorted = videoDiscovery.sortByEngagement(
                allVideos,
                SortMode.Trending,
                state.timePeriod.seconds,
            )
            _uiState.value = _uiState.value.copy(videos = sorted)
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Engagement sort failed: ${e.message}")
        }
    }

    /**
     * Most Zapped: sorted by total zap amount (sats), filtered to videos with actual zaps.
     * Mirrors web app's discoverPopularVideos with sortBy=zaps.
     */
    private suspend fun loadMostZappedFeed(state: HomeUiState) {
        val raw = videoRepository.fetchVideos(hashtag = state.selectedTag, limit = 300)
        allVideos = raw.distinctBy { it.id }
        val pubkeys = allVideos.map { it.pubkey }.distinct()
        allProfiles = profileRepository.getProfiles(pubkeys)

        // Show something while we wait
        _uiState.value = _uiState.value.copy(
            videos = allVideos.sortedByDescending { it.publishedAt },
            profiles = allProfiles,
            isLoading = false,
        )

        // Fetch real zap data + re-sort
        try {
            val sorted = videoDiscovery.sortByEngagement(
                allVideos,
                SortMode.MostZapped,
                state.timePeriod.seconds,
            )
            _uiState.value = _uiState.value.copy(
                videos = sorted,
                error = if (sorted.isEmpty()) "No zapped videos found for this time period" else null,
            )
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Zap sort failed: ${e.message}")
        }
    }

    /**
     * Following: videos from followed creators, newest first.
     */
    private suspend fun loadFollowingFeed(state: HomeUiState) {
        val follows = followRepository.getFollowedPubkeys()
        if (follows.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Follow some creators to see their videos here",
            )
            return
        }

        val videos = videoRepository.fetchVideos(authors = follows)
            .sortedByDescending { it.publishedAt }

        val pubkeys = videos.map { it.pubkey }.distinct()
        val profiles = profileRepository.getProfiles(pubkeys)
        allVideos = videos
        allProfiles = profiles

        _uiState.value = _uiState.value.copy(
            videos = videos,
            profiles = profiles,
            isLoading = false,
            error = if (videos.isEmpty()) "No videos from people you follow yet" else null,
        )
    }
}
