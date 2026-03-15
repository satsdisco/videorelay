package com.videorelay.app.ui.channel

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

data class ChannelUiState(
    val profile: Profile? = null,
    val videos: List<Video> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val videoRepository: VideoRepository,
    private val followRepository: FollowRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private var currentPubkey: String = ""

    fun loadChannel(pubkey: String) {
        currentPubkey = pubkey
        viewModelScope.launch {
            _uiState.value = ChannelUiState(isLoading = true)
            try {
                val profile = profileRepository.getProfile(pubkey)
                val videos = videoRepository.fetchVideos(authors = listOf(pubkey), limit = 50)
                    .sortedByDescending { it.publishedAt }
                val isFollowing = followRepository.isFollowing(pubkey)

                _uiState.value = ChannelUiState(
                    profile = profile,
                    videos = videos,
                    isFollowing = isFollowing,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = ChannelUiState(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val isNowFollowing = followRepository.toggleFollow(currentPubkey)
            _uiState.value = _uiState.value.copy(isFollowing = isNowFollowing)
        }
    }
}
