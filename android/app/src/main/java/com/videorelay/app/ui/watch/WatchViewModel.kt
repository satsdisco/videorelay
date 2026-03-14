package com.videorelay.app.ui.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.db.ViewHistoryDao
import com.videorelay.app.data.db.ViewHistoryEntity
import com.videorelay.app.data.nostr.*
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.RelayRepository
import com.videorelay.app.data.repository.VideoRepository
import com.videorelay.app.domain.model.Comment
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchUiState(
    val video: Video? = null,
    val creatorProfile: Profile? = null,
    val comments: List<Comment> = emptyList(),
    val commentProfiles: Map<String, Profile> = emptyMap(),
    val relatedVideos: List<Video> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class WatchViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val profileRepository: ProfileRepository,
    private val relayRepository: RelayRepository,
    private val relayPool: RelayPool,
    private val viewHistoryDao: ViewHistoryDao,
    val nip57Zap: NIP57Zap,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchUiState())
    val uiState: StateFlow<WatchUiState> = _uiState.asStateFlow()

    fun loadVideo(videoId: String) {
        viewModelScope.launch {
            _uiState.value = WatchUiState(isLoading = true)

            try {
                // Fetch video event from relays
                val relays = relayRepository.getActiveRelays()
                val filter = NostrFilter(
                    ids = listOf(videoId),
                    limit = 1,
                )
                val events = relayPool.query(relays, filter)
                val event = events.firstOrNull()

                val video = event?.let { NIP71Parser.parse(it) }
                if (video == null) {
                    _uiState.value = WatchUiState(isLoading = false, error = "Video not found")
                    return@launch
                }

                // Record view
                viewHistoryDao.upsert(ViewHistoryEntity(videoId = videoId))

                // Fetch creator profile
                val profile = profileRepository.getProfile(video.pubkey)

                _uiState.value = WatchUiState(
                    video = video,
                    creatorProfile = profile,
                    isLoading = false,
                )

                // Background: fetch comments
                loadComments(videoId, relays)

                // Background: fetch related videos (same author)
                val related = videoRepository.fetchVideos(
                    authors = listOf(video.pubkey),
                    limit = 10,
                ).filter { it.id != videoId }
                _uiState.value = _uiState.value.copy(relatedVideos = related)

            } catch (e: Exception) {
                _uiState.value = WatchUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load video",
                )
            }
        }
    }

    private suspend fun loadComments(videoId: String, relays: List<String>) {
        try {
            val filter = NostrFilter(
                kinds = listOf(NostrConstants.COMMENT_KIND),
                tags = mapOf("#e" to listOf(videoId)),
                limit = 100,
            )
            val events = relayPool.query(relays, filter)

            val comments = events.map { event ->
                Comment(
                    id = event.id,
                    pubkey = event.pubkey,
                    content = event.content,
                    publishedAt = event.created_at,
                    replyTo = event.getTag("e"),
                )
            }.sortedByDescending { it.publishedAt }

            val pubkeys = comments.map { it.pubkey }.distinct()
            val profiles = profileRepository.getProfiles(pubkeys)

            _uiState.value = _uiState.value.copy(
                comments = comments,
                commentProfiles = profiles,
            )
        } catch (_: Exception) {}
    }
}
