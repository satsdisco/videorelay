package com.videorelay.app.ui.home

import android.util.Log
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

enum class FeedTab { Home, Trending, MostZapped, Following }

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val profiles: Map<String, Profile> = emptyMap(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedTab: FeedTab = FeedTab.Home,
    val selectedTag: String? = null,
    val hasMore: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository,
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

        // If we already have videos, just re-sort (instant tab switch)
        if (allVideos.isNotEmpty()) {
            val sorted = sortVideos(allVideos, tab)
            _uiState.value = _uiState.value.copy(videos = sorted)

            // Fetch zap counts in background if switching to zap-based tab
            if ((tab == FeedTab.MostZapped || tab == FeedTab.Trending) && !zapCountsFetched) {
                fetchZapsAndResort(tab)
            }
        } else {
            loadVideos()
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
                val sorted = sortVideos(allVideos, state.selectedTab)

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
                // Fetch from relays
                val videos = videoRepository.fetchVideos(
                    hashtag = state.selectedTag,
                )

                allVideos = videos.distinctBy { it.id }
                val sorted = sortVideos(allVideos, state.selectedTab)

                // Fetch profiles
                val pubkeys = sorted.map { it.pubkey }.distinct()
                allProfiles = profileRepository.getProfiles(pubkeys)

                _uiState.value = _uiState.value.copy(
                    videos = sorted,
                    profiles = allProfiles,
                    isLoading = false,
                    hasMore = sorted.size >= 50,
                )

                // Background: fetch zap counts for all videos
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
                    val reSorted = sortVideos(allVideos, tab)
                    _uiState.value = _uiState.value.copy(videos = reSorted)
                }
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Zap count fetch failed: ${e.message}")
            }
        }
    }

    private fun sortVideos(videos: List<Video>, tab: FeedTab): List<Video> {
        return when (tab) {
            FeedTab.Home -> videos.sortedByDescending { it.publishedAt }
            FeedTab.Trending -> {
                // Trending: engagement-weighted recency
                // Recent videos with zaps rank higher than old videos with many zaps
                val now = System.currentTimeMillis() / 1000
                videos.sortedByDescending { v ->
                    val ageHours = ((now - v.publishedAt) / 3600.0).coerceAtLeast(1.0)
                    val score = (v.zapCount + 1).toDouble() / ageHours
                    score
                }
            }
            FeedTab.MostZapped -> {
                // Pure zap count ranking
                videos.sortedByDescending { it.zapCount }
            }
            FeedTab.Following -> {
                // TODO: filter by follow list when signed in
                // For now, show same as home with a hint
                videos.sortedByDescending { it.publishedAt }
            }
        }
    }
}
