package com.videorelay.app.ui.channel

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

data class ChannelUiState(
    val profile: Profile? = null,
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    fun loadChannel(pubkey: String) {
        viewModelScope.launch {
            _uiState.value = ChannelUiState(isLoading = true)
            try {
                val profile = profileRepository.getProfile(pubkey)
                val videos = videoRepository.fetchVideos(authors = listOf(pubkey), limit = 50)
                    .sortedByDescending { it.publishedAt }

                _uiState.value = ChannelUiState(
                    profile = profile,
                    videos = videos,
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
}
