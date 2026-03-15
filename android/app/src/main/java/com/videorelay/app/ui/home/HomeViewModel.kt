package com.videorelay.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.NsecSigner
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
    private val nsecSigner: NsecSigner,
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
     * Home feed: show cached videos INSTANTLY, then refresh.
     * Curated creators fetched SEPARATELY and guaranteed to appear at top.
     * Mirrors web app's Index.tsx home view logic.
     */
    private suspend fun loadHomeFeed(state: HomeUiState) {
        // Step 1: Show cached videos immediately
        val cached = videoRepository.getCachedVideos(100)
        if (cached.isNotEmpty()) {
            allVideos = cached.sortedByDescending { it.publishedAt }
            allProfiles = profileRepository.getProfiles(allVideos.map { it.pubkey }.distinct())
            _uiState.value = _uiState.value.copy(
                videos = allVideos,
                profiles = allProfiles,
                isLoading = false,
            )
        }

        try {
            // Step 2: Fetch curated pubkeys (always from relay, not just cache)
            val curatedPubkeys = try {
                curatedRepository.fetchFromRelays()
                    .ifEmpty { curatedRepository.getCuratedPubkeys() }
            } catch (_: Exception) { curatedRepository.getCuratedPubkeys() }

            // Step 3: Fetch curated creator videos DIRECTLY (guaranteed to appear)
            val curatedVideos = if (curatedPubkeys.isNotEmpty()) {
                try {
                    videoRepository.fetchVideos(
                        authors = curatedPubkeys,
                        limit = 50,
                    ).sortedByDescending { it.publishedAt }
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            // Step 4: Fetch general recent feed
            val recentVideos = videoRepository.fetchVideos(
                hashtag = state.selectedTag,
                limit = 200,
            )

            // Step 5: Curated first, then recent (deduped)
            val curatedSet = curatedVideos.map { it.id }.toSet()
            val combined = (curatedVideos + recentVideos.filter { it.id !in curatedSet })
                .distinctBy { it.id }
            allVideos = combined

            // Show immediately
            _uiState.value = _uiState.value.copy(
                videos = allVideos,
                isLoading = false,
                hasMore = allVideos.size >= 50,
            )

            // Step 6: Load profiles in background (non-blocking)
            allProfiles = profileRepository.getProfiles(allVideos.map { it.pubkey }.distinct())
            _uiState.value = _uiState.value.copy(profiles = allProfiles)

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (allVideos.isEmpty()) "Couldn't load videos — check relay connection" else null,
            )
        }
    }

    /**
     * Trending: multi-strategy fetch (time windows + tags + big query) then engagement sort.
     * Mirrors web app's useTrendingVideos + discoverPopularVideos(sortBy=trending).
     */
    private suspend fun loadTrendingFeed(state: HomeUiState) {
        // Show loading state
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Fetch broad set using multi-strategy (time windows + tags + big query)
        val raw = videoDiscovery.fetchBroadVideoSet(
            limit = 300,
            timePeriodSeconds = state.timePeriod.seconds,
        )
        allVideos = raw.distinctBy { it.id }
        val pubkeys = allVideos.map { it.pubkey }.distinct()
        allProfiles = profileRepository.getProfiles(pubkeys)

        // Show recency-sorted while engagement loads
        _uiState.value = _uiState.value.copy(
            videos = allVideos.sortedByDescending { it.publishedAt },
            profiles = allProfiles,
            isLoading = false,
        )

        // Re-sort by engagement in background
        try {
            val sorted = videoDiscovery.sortByEngagement(allVideos, SortMode.Trending, state.timePeriod.seconds)
            _uiState.value = _uiState.value.copy(videos = sorted)
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Trending sort failed: ${e.message}")
        }
    }

    /**
     * Most Zapped: fetch zap receipts FIRST, then get the videos they reference.
     * This guarantees only videos with actual zaps appear.
     */
    private suspend fun loadMostZappedFeed(state: HomeUiState) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        try {
            val zappedVideos = videoDiscovery.fetchMostZappedVideos(state.timePeriod.seconds)

            if (zappedVideos.isEmpty()) {
                // Fallback: broad fetch + engagement sort
                val raw = videoDiscovery.fetchBroadVideoSet(300, state.timePeriod.seconds)
                allVideos = raw.distinctBy { it.id }
                val pubkeys = allVideos.map { it.pubkey }.distinct()
                allProfiles = profileRepository.getProfiles(pubkeys)
                _uiState.value = _uiState.value.copy(
                    videos = allVideos,
                    profiles = allProfiles,
                    isLoading = false,
                    error = "Couldn't find zapped videos — showing recent",
                )
                val sorted = videoDiscovery.sortByEngagement(allVideos, SortMode.MostZapped, state.timePeriod.seconds)
                _uiState.value = _uiState.value.copy(videos = sorted, error = null)
            } else {
                allVideos = zappedVideos
                val pubkeys = allVideos.map { it.pubkey }.distinct()
                allProfiles = profileRepository.getProfiles(pubkeys)
                _uiState.value = _uiState.value.copy(
                    videos = allVideos,
                    profiles = allProfiles,
                    isLoading = false,
                )
            }
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Most zapped feed failed: ${e.message}")
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
        }
    }

    /**
     * Following: videos from followed creators, newest first.
     * Fetches your kind 3 contact list from relays to get the actual follow list.
     */
    private suspend fun loadFollowingFeed(state: HomeUiState) {
        // getFollowedPubkeys() now auto-fetches from relay if cache empty
        val follows = try {
            followRepository.getFollowedPubkeys()
        } catch (e: Exception) {
            Log.w("HomeViewModel", "Follow list fetch failed: ${e.message}")
            emptyList()
        }

        if (follows.isEmpty()) {
            val isLoggedIn = nsecSigner.isLoggedIn()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = if (isLoggedIn)
                    "Your follow list is empty or couldn't be loaded. Try following some creators from their channel pages."
                else
                    "Sign in to see videos from creators you follow.",
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
