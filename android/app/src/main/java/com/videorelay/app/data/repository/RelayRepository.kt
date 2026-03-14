package com.videorelay.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.videorelay.app.data.nostr.NostrConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.relayDataStore by preferencesDataStore(name = "relays")

@Serializable
data class RelayEntry(
    val url: String,
    val enabled: Boolean = true,
)

@Singleton
class RelayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("relay_list")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getRelays(): List<RelayEntry> {
        val stored = context.relayDataStore.data
            .map { prefs -> prefs[key] }
            .first()

        return if (stored != null) {
            try {
                json.decodeFromString<List<RelayEntry>>(stored)
            } catch (e: Exception) {
                defaultRelays()
            }
        } else {
            defaultRelays()
        }
    }

    suspend fun getActiveRelays(): List<String> {
        return getRelays().filter { it.enabled }.map { it.url }
    }

    suspend fun saveRelays(relays: List<RelayEntry>) {
        context.relayDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(RelayEntry.serializer()), relays)
        }
    }

    suspend fun addRelay(url: String): Boolean {
        val normalized = url.trim().trimEnd('/')
        if (!normalized.startsWith("wss://") && !normalized.startsWith("ws://")) return false

        val current = getRelays().toMutableList()
        if (current.any { it.url == normalized }) return false

        current.add(RelayEntry(normalized, true))
        saveRelays(current)
        return true
    }

    suspend fun removeRelay(url: String) {
        val current = getRelays().filter { it.url != url }
        saveRelays(current)
    }

    suspend fun toggleRelay(url: String) {
        val current = getRelays().map {
            if (it.url == url) it.copy(enabled = !it.enabled) else it
        }
        saveRelays(current)
    }

    suspend fun resetToDefaults() {
        saveRelays(defaultRelays())
    }

    private fun defaultRelays() = NostrConstants.DEFAULT_RELAYS.map { RelayEntry(it, true) }
}
