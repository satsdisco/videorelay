package com.videorelay.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.videorelay.app.data.nostr.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.curatedStore by preferencesDataStore(name = "curated")

/**
 * Fetches the curated creators list from a NIP-51 kind 30001 event.
 * Matches web app's curatedCreators.ts logic exactly.
 */
@Singleton
class CuratedRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
) {
    companion object {
        private val KEY_PUBKEYS = stringPreferencesKey("curated_pubkeys")
        private const val LIST_D_TAG = "videorelay-curated"
        private const val LIST_KIND = 30001

        // satsdisco — the admin who manages the curated list
        private const val CURATOR_PUBKEY = "47276eb163fc54b3733930ab5cfd5fa94687a1953871a873ad4faee91e8a5f38"

        // Hardcoded fallback if relay fetch fails
        private val FALLBACK_PUBKEYS = listOf(
            "7807080250235b13c8fb67aeaf340f24c33845b2470e7ddc7ec0d40c00682645",
        )
    }

    private var cachedPubkeys: List<String> = emptyList()
    private var loaded = false

    /** Get curated pubkeys — from cache or relay */
    suspend fun getCuratedPubkeys(): List<String> {
        if (!loaded) loadFromStore()
        if (cachedPubkeys.isEmpty()) {
            fetchFromRelays()
        }
        return cachedPubkeys.ifEmpty { FALLBACK_PUBKEYS }
    }

    /** Fetch curated list from relays (NIP-51 kind 30001) */
    suspend fun fetchFromRelays(): List<String> {
        return try {
            val relays = relayRepository.getActiveRelays()
                .filter { "nostr.band" in it || "primal" in it || "damus" in it }
                .ifEmpty { relayRepository.getActiveRelays().take(4) }

            val filter = NostrFilter(
                kinds = listOf(LIST_KIND),
                authors = listOf(CURATOR_PUBKEY),
                tags = mapOf("#d" to listOf(LIST_D_TAG)),
                limit = 1,
            )

            val events = relayPool.query(relays, filter, timeoutMs = 5000)
            val latest = events.maxByOrNull { it.created_at } ?: return cachedPubkeys.ifEmpty { FALLBACK_PUBKEYS }

            val pubkeys = latest.getAllTags("p").filter { it.length == 64 }
            cachedPubkeys = pubkeys
            loaded = true
            persist(pubkeys)
            pubkeys
        } catch (_: Exception) {
            cachedPubkeys.ifEmpty { FALLBACK_PUBKEYS }
        }
    }

    private suspend fun loadFromStore() {
        val prefs = context.curatedStore.data.first()
        val stored = prefs[KEY_PUBKEYS]
        if (!stored.isNullOrBlank()) {
            cachedPubkeys = stored.split(",").filter { it.isNotBlank() }
        }
        loaded = true
    }

    private suspend fun persist(pubkeys: List<String>) {
        context.curatedStore.edit { prefs ->
            prefs[KEY_PUBKEYS] = pubkeys.joinToString(",")
        }
    }
}
