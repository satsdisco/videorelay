package com.videorelay.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.*
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.RelayRepository
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Video> = emptyList(),
    val profiles: Map<String, Profile> = emptyMap(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)

        // Debounced search
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(400) // debounce
                search(query)
            }
        }
    }

    fun submitSearch() {
        val query = _uiState.value.query
        if (query.isNotBlank()) {
            searchJob?.cancel()
            viewModelScope.launch { search(query) }
        }
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)

        try {
            val relays = relayRepository.getActiveRelays()

            // Search via relay search extension (NIP-50)
            val filter = NostrFilter(
                kinds = NostrConstants.ALL_VIDEO_KINDS,
                search = query,
                limit = 50,
            )

            val events = relayPool.query(relays, filter)
            val videos = events.mapNotNull { NIP71Parser.parse(it) }
                .distinctBy { it.id }
                .sortedByDescending { it.publishedAt }

            val pubkeys = videos.map { it.pubkey }.distinct()
            val profiles = profileRepository.getProfiles(pubkeys)

            _uiState.value = _uiState.value.copy(
                results = videos,
                profiles = profiles,
                isSearching = false,
                hasSearched = true,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                hasSearched = true,
            )
        }
    }
}
