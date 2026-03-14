package com.videorelay.app.data.repository

import com.videorelay.app.data.db.ProfileDao
import com.videorelay.app.data.db.ProfileEntity
import com.videorelay.app.data.nostr.*
import com.videorelay.app.domain.model.Profile
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
        private const val CACHE_MAX_AGE_MS = 60 * 60 * 1000L // 1 hour
    }

    suspend fun getProfile(pubkey: String): Profile? {
        // Check cache first
        profileDao.getByPubkey(pubkey)?.let { cached ->
            if (System.currentTimeMillis() - cached.fetchedAt < CACHE_MAX_AGE_MS) {
                return cached.toDomain()
            }
        }

        // Fetch from relays
        val relays = relayRepository.getActiveRelays()
        val filter = NostrFilter(
            kinds = listOf(NostrConstants.PROFILE_KIND),
            authors = listOf(pubkey),
            limit = 1,
        )

        val events = relayPool.query(relays, filter)
        val event = events.maxByOrNull { it.created_at } ?: return null

        val profile = parseProfileEvent(event) ?: return null
        profileDao.insertAll(listOf(ProfileEntity.from(profile)))
        return profile
    }

    suspend fun getProfiles(pubkeys: List<String>): Map<String, Profile> {
        if (pubkeys.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, Profile>()
        val toFetch = mutableListOf<String>()

        // Check cache
        val cached = profileDao.getByPubkeys(pubkeys)
        for (entity in cached) {
            if (System.currentTimeMillis() - entity.fetchedAt < CACHE_MAX_AGE_MS) {
                result[entity.pubkey] = entity.toDomain()
            } else {
                toFetch.add(entity.pubkey)
            }
        }
        toFetch.addAll(pubkeys.filter { it !in result && it !in toFetch })

        if (toFetch.isEmpty()) return result

        // Fetch missing from relays
        val relays = relayRepository.getActiveRelays()
        val filter = NostrFilter(
            kinds = listOf(NostrConstants.PROFILE_KIND),
            authors = toFetch,
            limit = toFetch.size,
        )

        val events = relayPool.query(relays, filter)
        val profileEntities = mutableListOf<ProfileEntity>()

        // Group by pubkey, take most recent
        events.groupBy { it.pubkey }
            .mapValues { (_, evts) -> evts.maxByOrNull { it.created_at }!! }
            .forEach { (_, event) ->
                parseProfileEvent(event)?.let { profile ->
                    result[profile.pubkey] = profile
                    profileEntities.add(ProfileEntity.from(profile))
                }
            }

        if (profileEntities.isNotEmpty()) {
            profileDao.insertAll(profileEntities)
        }

        return result
    }

    private fun parseProfileEvent(event: NostrEvent): Profile? {
        return try {
            val json = Json.parseToJsonElement(event.content).jsonObject
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

private fun ProfileEntity.Companion.from(profile: Profile) = ProfileEntity(
    pubkey = profile.pubkey,
    name = profile.name,
    displayName = profile.displayName,
    picture = profile.picture,
    banner = profile.banner,
    about = profile.about,
    lud16 = profile.lud16,
    lud06 = profile.lud06,
    nip05 = profile.nip05,
)

// Add companion to ProfileEntity
private val ProfileEntity.Companion: ProfileEntityCompanion get() = ProfileEntityCompanion
private object ProfileEntityCompanion {
    fun from(profile: Profile) = ProfileEntity(
        pubkey = profile.pubkey,
        name = profile.name,
        displayName = profile.displayName,
        picture = profile.picture,
        banner = profile.banner,
        about = profile.about,
        lud16 = profile.lud16,
        lud06 = profile.lud06,
        nip05 = profile.nip05,
    )
}
