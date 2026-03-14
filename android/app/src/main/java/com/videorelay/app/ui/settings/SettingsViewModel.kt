package com.videorelay.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.AmberSigner
import com.videorelay.app.data.repository.RelayEntry
import com.videorelay.app.data.repository.RelayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val relays: List<RelayEntry> = emptyList(),
    val isAmberAvailable: Boolean = false,
    val loggedInPubkey: String? = null,
    val isDarkTheme: Boolean = true,
    val isDynamicColor: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val relayRepository: RelayRepository,
    private val amberSigner: AmberSigner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val relays = relayRepository.getRelays()
            val amberAvailable = amberSigner.isAvailable()

            _uiState.value = SettingsUiState(
                relays = relays,
                isAmberAvailable = amberAvailable,
                loggedInPubkey = amberSigner.publicKey,
            )
        }
    }

    fun toggleRelay(url: String) {
        viewModelScope.launch {
            relayRepository.toggleRelay(url)
            _uiState.value = _uiState.value.copy(relays = relayRepository.getRelays())
        }
    }

    fun addRelay(url: String) {
        viewModelScope.launch {
            relayRepository.addRelay(url)
            _uiState.value = _uiState.value.copy(relays = relayRepository.getRelays())
        }
    }

    fun removeRelay(url: String) {
        viewModelScope.launch {
            relayRepository.removeRelay(url)
            _uiState.value = _uiState.value.copy(relays = relayRepository.getRelays())
        }
    }

    fun resetRelays() {
        viewModelScope.launch {
            relayRepository.resetToDefaults()
            _uiState.value = _uiState.value.copy(relays = relayRepository.getRelays())
        }
    }
}
