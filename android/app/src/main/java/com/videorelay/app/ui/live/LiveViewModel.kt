package com.videorelay.app.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.*
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.RelayRepository
import com.videorelay.app.domain.model.LiveStream
import com.videorelay.app.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveUiState(
    val streams: List<LiveStream> = emptyList(),
    val profiles: Map<String, Profile> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        loadStreams()
    }

    fun refresh() = loadStreams()

    private fun loadStreams() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val relays = relayRepository.getActiveRelays()
                val filter = NostrFilter(
                    kinds = listOf(NostrConstants.LIVE_EVENT_KIND),
                    limit = 50,
                )

                val events = relayPool.query(relays, filter)
                val streams = events.mapNotNull { event ->
                    val status = event.getTag("status") ?: "live"
                    val streamUrl = event.getTag("streaming") ?: event.getTag("recording") ?: return@mapNotNull null

                    LiveStream(
                        id = event.id,
                        pubkey = event.pubkey,
                        title = event.getTag("title") ?: event.getTag("subject") ?: "Untitled Stream",
                        summary = event.getTag("summary") ?: event.content,
                        thumbnail = event.getTag("image") ?: event.getTag("thumb") ?: "",
                        streamUrl = streamUrl,
                        status = status,
                        starts = event.getTag("starts")?.toLongOrNull(),
                        ends = event.getTag("ends")?.toLongOrNull(),
                        currentParticipants = event.getTag("current_participants")?.toIntOrNull() ?: 0,
                        tags = event.getAllTags("t"),
                        publishedAt = event.created_at,
                    )
                }
                    .sortedWith(compareBy<LiveStream> { it.status != "live" }.thenByDescending { it.publishedAt })

                val pubkeys = streams.map { it.pubkey }.distinct()
                val profiles = profileRepository.getProfiles(pubkeys)

                _uiState.value = LiveUiState(
                    streams = streams,
                    profiles = profiles,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
    }
}
