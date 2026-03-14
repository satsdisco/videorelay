package com.videorelay.app.ui.home

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

    init {
        loadVideos()
    }

    fun selectTab(tab: FeedTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, selectedTag = null)
        loadVideos()
    }

    fun selectTag(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        loadVideos()
    }

    fun refresh() {
        loadVideos()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.videos.isEmpty()) return

        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val oldest = state.videos.minOf { it.publishedAt }
                val moreVideos = videoRepository.fetchVideos(
                    limit = 100,
                    hashtag = state.selectedTag,
                    until = oldest - 1,
                )

                val allVideos = (state.videos + moreVideos).distinctBy { it.id }

                // Fetch profiles for new videos
                val newPubkeys = moreVideos.map { it.pubkey }.distinct()
                    .filter { it !in state.profiles }
                val newProfiles = profileRepository.getProfiles(newPubkeys)

                _uiState.value = state.copy(
                    videos = allVideos,
                    profiles = state.profiles + newProfiles,
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
                // Show cached immediately
                val cached = videoRepository.getCachedVideos()
                if (cached.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        videos = cached,
                        isLoading = false,
                    )
                }

                // Fetch fresh from relays
                val videos = videoRepository.fetchVideos(
                    hashtag = state.selectedTag,
                )

                val sorted = when (state.selectedTab) {
                    FeedTab.Home -> videos.sortedByDescending { it.publishedAt }
                    FeedTab.Trending -> videos.sortedByDescending { it.publishedAt } // TODO: engagement scoring
                    FeedTab.MostZapped -> videos.sortedByDescending { it.zapCount }
                    FeedTab.Following -> videos // TODO: filter by follow list
                }

                // Fetch profiles
                val pubkeys = sorted.map { it.pubkey }.distinct()
                val profiles = profileRepository.getProfiles(pubkeys)

                _uiState.value = _uiState.value.copy(
                    videos = sorted,
                    profiles = profiles,
                    isLoading = false,
                    hasMore = sorted.size >= 50,
                )

                // Background: fetch zap counts and re-sort
                val zapCounts = videoRepository.fetchZapCounts(sorted)
                if (zapCounts.isNotEmpty()) {
                    val updated = sorted.map { v ->
                        zapCounts[v.id]?.let { v.copy(zapCount = it) } ?: v
                    }
                    val reSorted = when (state.selectedTab) {
                        FeedTab.MostZapped -> updated.sortedByDescending { it.zapCount }
                        else -> updated
                    }
                    _uiState.value = _uiState.value.copy(videos = reSorted)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load videos",
                )
            }
        }
    }
}
