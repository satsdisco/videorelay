package com.videorelay.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.repository.FollowRepository
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

enum class FeedTab { Home, Trending, MostZapped, Following }

enum class TimePeriod(val label: String, val seconds: Long) {
    Today("Today", 86_400),
    Week("Week", 604_800),
    Month("Month", 2_592_000),
    All("All time", Long.MAX_VALUE),
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Cache all fetched videos so tab switching is instant
    private var allVideos: List<Video> = emptyList()
    private var allProfiles: Map<String, Profile> = emptyMap()
    private var zapCountsFetched = false

    init {
        loadVideos()
    }

    fun selectTab(tab: FeedTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.value = _uiState.value.copy(selectedTab = tab, selectedTag = null)

        if (tab == FeedTab.Following) {
            loadFollowingFeed()
        } else if (allVideos.isNotEmpty()) {
            val sorted = applyFiltersAndSort(allVideos, tab, _uiState.value.timePeriod)
            _uiState.value = _uiState.value.copy(videos = sorted, error = null)
            if ((tab == FeedTab.MostZapped || tab == FeedTab.Trending) && !zapCountsFetched) {
                fetchZapsAndResort(tab)
            }
        } else {
            loadVideos()
        }
    }

    fun selectTimePeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(timePeriod = period)
        val sorted = applyFiltersAndSort(allVideos, _uiState.value.selectedTab, period)
        _uiState.value = _uiState.value.copy(videos = sorted)
    }

    private fun loadFollowingFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, videos = emptyList())
            try {
                val follows = followRepository.getFollowedPubkeys()
                if (follows.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Follow some creators to see their videos here",
                    )
                    return@launch
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
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load following feed",
                )
            }
        }
    }

    fun selectTag(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        loadVideos()
    }

    fun refresh() {
        zapCountsFetched = false
        loadVideos()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.videos.isEmpty()) return

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
                val sorted = applyFiltersAndSort(allVideos, state.selectedTab, state.timePeriod)

                val newPubkeys = moreVideos.map { it.pubkey }.distinct()
                    .filter { it !in allProfiles }
                val newProfiles = profileRepository.getProfiles(newPubkeys)
                allProfiles = allProfiles + newProfiles

                _uiState.value = state.copy(
                    videos = sorted,
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
        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val videos = videoRepository.fetchVideos(hashtag = state.selectedTag)
                allVideos = videos.distinctBy { it.id }
                val sorted = applyFiltersAndSort(allVideos, state.selectedTab, state.timePeriod)
                val pubkeys = sorted.map { it.pubkey }.distinct()
                allProfiles = profileRepository.getProfiles(pubkeys)

                _uiState.value = _uiState.value.copy(
                    videos = sorted,
                    profiles = allProfiles,
                    isLoading = false,
                    hasMore = sorted.size >= 50,
                )

                fetchZapsAndResort(state.selectedTab)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load videos", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load videos",
                )
            }
        }
    }

    private fun fetchZapsAndResort(tab: FeedTab) {
        viewModelScope.launch {
            try {
                val zapCounts = videoRepository.fetchZapCounts(allVideos)
                if (zapCounts.isNotEmpty()) {
                    zapCountsFetched = true
                    allVideos = allVideos.map { v ->
                        zapCounts[v.id]?.let { v.copy(zapCount = it) } ?: v
                    }
                    val reSorted = applyFiltersAndSort(allVideos, _uiState.value.selectedTab, _uiState.value.timePeriod)
                    _uiState.value = _uiState.value.copy(videos = reSorted)
                }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Zap count fetch failed: ${e.message}")
            }
        }
    }

    private fun applyFiltersAndSort(videos: List<Video>, tab: FeedTab, period: TimePeriod): List<Video> {
        val now = System.currentTimeMillis() / 1000
        val filtered = if (period == TimePeriod.All) videos
        else videos.filter { it.publishedAt >= now - period.seconds }

        return when (tab) {
            FeedTab.Home -> filtered.sortedByDescending { it.publishedAt }
            FeedTab.Trending -> {
                filtered.sortedByDescending { v ->
                    val ageHours = ((now - v.publishedAt) / 3600.0).coerceAtLeast(1.0)
                    (v.zapCount + 1).toDouble() / ageHours
                }
            }
            FeedTab.MostZapped -> filtered.sortedByDescending { it.zapCount }
            FeedTab.Following -> filtered.sortedByDescending { it.publishedAt }
        }
    }
}

