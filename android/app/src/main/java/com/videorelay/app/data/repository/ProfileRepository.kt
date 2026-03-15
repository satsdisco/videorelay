package com.videorelay.app.data.repository

import android.util.Log
import com.videorelay.app.data.db.ProfileDao
import com.videorelay.app.data.db.ProfileEntity
import com.videorelay.app.data.nostr.*
import com.videorelay.app.domain.model.Profile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val relayPool: RelayPool,
    private val profileDao: ProfileDao,
    private val relayRepository: RelayRepository,
) {
    companion object {
        private const val TAG = "ProfileRepository"
        private const val CACHE_MAX_AGE_MS = 60 * 60 * 1000L // 1 hour

        // Fast relays for profile fetches — aggregators with the best coverage
        private val PROFILE_RELAYS = listOf(
            "wss://relay.nostr.band",
            "wss://relay.primal.net",
            "wss://purplepag.es",
        )
    }

    // In-memory profile cache — avoids repeated DataStore + relay queries
    private val memoryCache = mutableMapOf<String, Profile>()

    suspend fun getProfile(pubkey: String): Profile? {
        // 1. Memory cache (instant)
        memoryCache[pubkey]?.let { return it }

        // 2. DB cache
        profileDao.getByPubkey(pubkey)?.let { cached ->
            if (System.currentTimeMillis() - cached.fetchedAt < CACHE_MAX_AGE_MS) {
                val profile = cached.toDomain()
                memoryCache[pubkey] = profile
                return profile
            }
        }

        // 3. Relay fetch — use fast profile relays only
        return fetchFromRelays(listOf(pubkey)).values.firstOrNull()
    }

    suspend fun getProfiles(pubkeys: List<String>): Map<String, Profile> {
        if (pubkeys.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Profile>()
        val toFetch = mutableListOf<String>()

        // Memory cache first (instant)
        for (pk in pubkeys) {
            memoryCache[pk]?.let { result[pk] = it } ?: toFetch.add(pk)
        }
        if (toFetch.isEmpty()) return result

        // DB cache
        val dbCached = profileDao.getByPubkeys(toFetch)
        val stillNeeded = mutableListOf<String>()
        for (entity in dbCached) {
            if (System.currentTimeMillis() - entity.fetchedAt < CACHE_MAX_AGE_MS) {
                val profile = entity.toDomain()
                result[entity.pubkey] = profile
                memoryCache[entity.pubkey] = profile
            } else {
                stillNeeded.add(entity.pubkey)
            }
        }
        stillNeeded.addAll(toFetch.filter { it !in result })

        if (stillNeeded.isEmpty()) return result

        // Relay fetch — batch all needed profiles in ONE query
        val fetched = fetchFromRelays(stillNeeded)
        result.putAll(fetched)
        return result
    }

    private suspend fun fetchFromRelays(pubkeys: List<String>): Map<String, Profile> {
        if (pubkeys.isEmpty()) return emptyMap()

        return try {
            val filter = NostrFilter(
                kinds = listOf(NostrConstants.PROFILE_KIND),
                authors = pubkeys,
                limit = pubkeys.size.coerceAtLeast(50),
            )

            // Use profile-specific fast relays (not all 10 relays)
            val events = relayPool.query(PROFILE_RELAYS, filter, timeoutMs = 5000)

            val result = mutableMapOf<String, Profile>()
            val entities = mutableListOf<ProfileEntity>()

            events.groupBy { it.pubkey }
                .mapValues { (_, evts) -> evts.maxByOrNull { it.created_at }!! }
                .forEach { (pk, event) ->
                    parseProfileEvent(event)?.let { profile ->
                        result[pk] = profile
                        memoryCache[pk] = profile
                        entities.add(profile.toEntity())
                    }
                }

            if (entities.isNotEmpty()) profileDao.insertAll(entities)
            result
        } catch (e: Exception) {
            Log.w(TAG, "Profile fetch failed: ${e.message}")
            emptyMap()
        }
    }

    fun parseProfileEvent(event: NostrEvent): Profile? {
        return try {
            val json = Json { ignoreUnknownKeys = true }
                .parseToJsonElement(event.content).jsonObject
            Profile(
                pubkey = event.pubkey,
                name = json["name"]?.jsonPrimitive?.content ?: "",
                displayName = json["display_name"]?.jsonPrimitive?.content
                    ?: json["displayName"]?.jsonPrimitive?.content ?: "",
                picture = json["picture"]?.jsonPrimitive?.content ?: "",
                banner = json["banner"]?.jsonPrimitive?.content ?: "",
                about = json["about"]?.jsonPrimitive?.content ?: "",
                lud16 = json["lud16"]?.jsonPrimitive?.content ?: "",
                lud06 = json["lud06"]?.jsonPrimitive?.content ?: "",
                nip05 = json["nip05"]?.jsonPrimitive?.content ?: "",
            )
        } catch (e: Exception) {
            null
        }
    }
}

private fun ProfileEntity.toDomain() = Profile(
    pubkey = pubkey,
    name = name,
    displayName = displayName,
    picture = picture,
    banner = banner,
    about = about,
    lud16 = lud16,
    lud06 = lud06,
    nip05 = nip05,
    fetchedAt = fetchedAt,
)

private fun Profile.toEntity() = ProfileEntity(
    pubkey = pubkey,
    name = name,
    displayName = displayName,
    picture = picture,
    banner = banner,
    about = about,
    lud16 = lud16,
    lud06 = lud06,
    nip05 = nip05,
)
