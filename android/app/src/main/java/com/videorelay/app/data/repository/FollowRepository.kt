package com.videorelay.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.videorelay.app.data.nostr.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.followStore by preferencesDataStore(name = "follow_list")

@Singleton
class FollowRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
    private val nsecSigner: NsecSigner,
) {
    companion object {
        private val KEY_FOLLOWS = stringPreferencesKey("follows")
    }

    // In-memory cache
    private var cachedFollows: MutableSet<String> = mutableSetOf()
    private var loaded = false

    /** Get the list of followed pubkeys — loads from relay if cache is empty */
    suspend fun getFollowedPubkeys(): List<String> {
        if (!loaded) load()
        // If local cache empty, try fetching from relay
        if (cachedFollows.isEmpty()) {
            return fetchFromRelays()
        }
        return cachedFollows.toList()
    }

    /** Check if a pubkey is followed */
    suspend fun isFollowing(pubkey: String): Boolean {
        if (!loaded) load()
        return pubkey in cachedFollows
    }

    /** Toggle follow/unfollow. Returns true if now following. */
    suspend fun toggleFollow(pubkey: String): Boolean {
        if (!loaded) load()
        return if (pubkey in cachedFollows) {
            cachedFollows.remove(pubkey)
            saveAndPublish()
            false
        } else {
            cachedFollows.add(pubkey)
            saveAndPublish()
            true
        }
    }

    /** Fetch follow list from relays for logged-in user */
    suspend fun fetchFromRelays(): List<String> {
        // Ensure credentials are loaded (handles Amber sign-in across app restarts)
        if (!nsecSigner.isLoggedIn()) return emptyList()
        val myPubkey = nsecSigner.publicKey ?: return emptyList()

        android.util.Log.d("FollowRepository", "Fetching follow list for $myPubkey")

        // Use fast aggregator relays that index kind 3 well
        val fastRelays = listOf(
            "wss://relay.nostr.band",
            "wss://relay.primal.net",
            "wss://purplepag.es",
            "wss://relay.damus.io",
        )

        val filter = NostrFilter(
            kinds = listOf(NostrConstants.FOLLOW_LIST_KIND),
            authors = listOf(myPubkey),
            limit = 1,
        )

        val events = relayPool.query(fastRelays, filter, timeoutMs = 8000)
        android.util.Log.d("FollowRepository", "Got ${events.size} contact list events")

        val latestEvent = events.maxByOrNull { it.created_at } ?: run {
            android.util.Log.w("FollowRepository", "No contact list found for $myPubkey")
            return cachedFollows.toList()
        }

        val follows = latestEvent.getAllTags("p")
        android.util.Log.d("FollowRepository", "Found ${follows.size} followed pubkeys")

        cachedFollows = follows.toMutableSet()
        loaded = true
        persist()
        return follows
    }

    private suspend fun load() {
        // Try in-memory first, then DataStore
        val prefs = context.followStore.data.first()
        val stored = prefs[KEY_FOLLOWS]
        if (!stored.isNullOrBlank()) {
            cachedFollows = stored.split(",").filter { it.isNotBlank() }.toMutableSet()
        }
        loaded = true
    }

    private suspend fun persist() {
        context.followStore.edit { prefs ->
            prefs[KEY_FOLLOWS] = cachedFollows.joinToString(",")
        }
    }

    private suspend fun saveAndPublish() {
        persist()
        // Build and sign a new kind 3 contact list event
        val pTags = cachedFollows.map { listOf("p", it) }
        val event = UnsignedEvent(
            kind = NostrConstants.FOLLOW_LIST_KIND,
            content = "",
            tags = pTags,
        )
        val signed = nsecSigner.signEvent(event) ?: return

        // Publish to relays
        val relays = relayRepository.getActiveRelays()
        try {
            relayPool.publish(relays, signed)
        } catch (_: Exception) {}
    }
}
