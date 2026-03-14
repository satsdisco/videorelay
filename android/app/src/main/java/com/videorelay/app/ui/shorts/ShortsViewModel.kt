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
    val currentIndex: Int = 0,
)

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    init {
        loadShorts()
    }

    fun setCurrentIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    private fun loadShorts() {
        viewModelScope.launch {
            try {
                val shorts = videoRepository.fetchVideos(limit = 100)
                    .filter { it.isShort }
                    .shuffled()

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
}
