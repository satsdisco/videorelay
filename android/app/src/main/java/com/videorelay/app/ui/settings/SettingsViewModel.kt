package com.videorelay.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.nostr.AmberSigner
import com.videorelay.app.data.nostr.NsecSigner
import com.videorelay.app.data.repository.ProfileRepository
import com.videorelay.app.data.repository.RelayEntry
import com.videorelay.app.data.repository.RelayRepository
import com.videorelay.app.domain.model.Profile
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
    val authMethod: String? = null,
    val myProfile: Profile? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val relayRepository: RelayRepository,
    private val amberSigner: AmberSigner,
    private val nsecSigner: NsecSigner,
    private val profileRepository: ProfileRepository,
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
            nsecSigner.isLoggedIn()
            val method = nsecSigner.getAuthMethod()
            val pubkey = nsecSigner.publicKey

            _uiState.value = SettingsUiState(
                relays = relays,
                isAmberAvailable = amberAvailable,
                loggedInPubkey = pubkey,
                authMethod = method,
            )

            // Fetch profile if logged in — uses memory/DB cache, fast
            if (pubkey != null) {
                try {
                    val profile = profileRepository.getProfile(pubkey)
                    _uiState.value = _uiState.value.copy(myProfile = profile)
                } catch (_: Exception) {
                    // Still show pubkey even if profile fetch fails
                }
            }
        }
    }

    fun loginWithBunker(bunkerUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            _uiState.value = _uiState.value.copy(
                isLoggingIn = false,
                loginError = "Bunker (NIP-46) support coming soon.",
            )
        }
    }

    fun onAmberResult(pubkey: String) {
        viewModelScope.launch {
            amberSigner.setPublicKey(pubkey)
            nsecSigner.saveAmberPubkey(pubkey)
            _uiState.value = _uiState.value.copy(
                loggedInPubkey = pubkey,
                authMethod = "amber",
            )
            try {
                val profile = profileRepository.getProfile(pubkey)
                _uiState.value = _uiState.value.copy(myProfile = profile)
            } catch (_: Exception) {}
        }
    }

    fun loginWithNsec(nsecInput: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            val pubkey = nsecSigner.loginWithNsec(nsecInput)
            if (pubkey != null) {
                _uiState.value = _uiState.value.copy(
                    loggedInPubkey = pubkey,
                    authMethod = "nsec",
                    isLoggingIn = false,
                    loginError = null,
                )
                try {
                    val profile = profileRepository.getProfile(pubkey)
                    _uiState.value = _uiState.value.copy(myProfile = profile)
                } catch (_: Exception) {}
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoggingIn = false,
                    loginError = "Invalid nsec key. Check and try again.",
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            nsecSigner.logout()
            _uiState.value = _uiState.value.copy(
                loggedInPubkey = null,
                authMethod = null,
                myProfile = null,
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
