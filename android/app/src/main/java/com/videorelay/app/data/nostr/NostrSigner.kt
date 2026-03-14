package com.videorelay.app.data.nostr

/**
 * Abstraction for signing nostr events.
 * Implementations: AmberSigner (NIP-55), NsecSigner (local key).
 */
interface NostrSigner {
    val publicKey: String?
    suspend fun sign(event: UnsignedEvent): NostrEvent?
    suspend fun isAvailable(): Boolean
}
