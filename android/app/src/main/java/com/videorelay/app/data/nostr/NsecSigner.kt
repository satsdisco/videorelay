package com.videorelay.app.data.nostr

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.acinq.secp256k1.Secp256k1
import fr.acinq.secp256k1.jni.NativeSecp256k1AndroidLoader
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authStore by preferencesDataStore(name = "auth")

/**
 * Local nsec signer using real secp256k1 (fr.acinq.secp256k1-kmp).
 * Supports BIP-340 Schnorr signatures for Nostr events.
 */
@Singleton
class NsecSigner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "NsecSigner"
        private val KEY_NSEC = stringPreferencesKey("nsec")
        private val KEY_PUBKEY = stringPreferencesKey("pubkey")
        private val KEY_AUTH_METHOD = stringPreferencesKey("auth_method")
    }

    private var _privateKey: ByteArray? = null
    private var _publicKey: String? = null
    private var secp256k1: Secp256k1? = null

    private fun getSecp256k1(): Secp256k1 {
        return secp256k1 ?: try {
            NativeSecp256k1AndroidLoader.load().also { secp256k1 = it }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load secp256k1 JNI: ${e.message}")
            Secp256k1 // Fallback to companion (may fail without native lib)
        }
    }

    val publicKey: String? get() = _publicKey

    suspend fun loadSaved(): Boolean {
        return try {
            val prefs = context.authStore.data.first()
            val savedNsec = prefs[KEY_NSEC]
            val savedPubkey = prefs[KEY_PUBKEY]
            if (savedNsec != null && savedPubkey != null) {
                _privateKey = hexToBytes(savedNsec)
                _publicKey = savedPubkey
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saved credentials", e)
            false
        }
    }

    suspend fun loginWithNsec(input: String): String? {
        return try {
            val hexKey = if (input.startsWith("nsec1")) {
                bech32ToHex(input)
            } else {
                input.lowercase().trim()
            }

            if (hexKey == null || hexKey.length != 64) {
                Log.e(TAG, "Invalid key length: ${hexKey?.length}")
                return null
            }

            val privKeyBytes = hexToBytes(hexKey)

            // Derive x-only public key using secp256k1
            val crypto = getSecp256k1()
            val pubKeyBytes = crypto.pubkeyCreate(privKeyBytes)
            // x-only pubkey = drop first byte (parity) and take 32 bytes
            val xOnlyPubKey = pubKeyBytes.drop(1).take(32).toByteArray().toHex()

            _privateKey = privKeyBytes
            _publicKey = xOnlyPubKey

            context.authStore.edit { prefs ->
                prefs[KEY_NSEC] = hexKey
                prefs[KEY_PUBKEY] = xOnlyPubKey
                prefs[KEY_AUTH_METHOD] = "nsec"
            }

            xOnlyPubKey
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            null
        }
    }

    suspend fun saveAmberPubkey(pubkey: String) {
        _publicKey = pubkey
        context.authStore.edit { prefs ->
            prefs[KEY_PUBKEY] = pubkey
            prefs[KEY_AUTH_METHOD] = "amber"
        }
    }

    suspend fun getAuthMethod(): String? =
        context.authStore.data.first()[KEY_AUTH_METHOD]

    suspend fun logout() {
        _privateKey = null
        _publicKey = null
        context.authStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean {
        if (_publicKey != null) return true
        return loadSaved()
    }

    /**
     * Sign an event using BIP-340 Schnorr via fr.acinq.secp256k1.
     * Returns null if no private key (Amber auth mode).
     */
    fun signEvent(event: UnsignedEvent): NostrEvent? {
        val privKey = _privateKey ?: return null
        val pubkey = _publicKey ?: return null

        return try {
            // Serialize for ID: [0, pubkey, created_at, kind, tags, content]
            val serialized = buildString {
                append("[0,\"")
                append(pubkey)
                append("\",")
                append(event.created_at)
                append(",")
                append(event.kind)
                append(",")
                append(serializeTags(event.tags))
                append(",\"")
                append(escapeJson(event.content))
                append("\"]")
            }

            val id = sha256Hex(serialized.toByteArray(Charsets.UTF_8))
            val idBytes = hexToBytes(id)

            // BIP-340 Schnorr signature using Android JNI
            val aux = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val crypto = getSecp256k1()
            val sigBytes = crypto.signSchnorr(idBytes, privKey, aux)
            val sig = sigBytes.toHex()

            NostrEvent(
                id = id,
                pubkey = pubkey,
                created_at = event.created_at,
                kind = event.kind,
                tags = event.tags,
                content = event.content,
                sig = sig,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Signing failed", e)
            null
        }
    }

    // --- Serialization helpers ---

    private fun serializeTags(tags: List<List<String>>): String {
        return "[" + tags.joinToString(",") { tag ->
            "[" + tag.joinToString(",") { "\"${escapeJson(it)}\"" } + "]"
        } + "]"
    }

    private fun escapeJson(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).toHex()
    }

    // --- Bech32 (nsec decode) ---

    private fun bech32ToHex(bech32: String): String? {
        return try {
            val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
            val hrpEnd = bech32.lastIndexOf('1')
            if (hrpEnd < 1) return null
            val data = bech32.substring(hrpEnd + 1).map { CHARSET.indexOf(it) }
            if (data.any { it == -1 }) return null
            val payload = data.dropLast(6) // remove 6-char checksum
            val bytes = convertBits(payload, 5, 8, false) ?: return null
            bytes.toByteArray().toHex()
        } catch (e: Exception) {
            Log.e(TAG, "Bech32 decode failed", e)
            null
        }
    }

    private fun convertBits(data: List<Int>, fromBits: Int, toBits: Int, pad: Boolean): List<Int>? {
        var acc = 0; var bits = 0
        val result = mutableListOf<Int>()
        val maxv = (1 shl toBits) - 1
        for (value in data) {
            if (value < 0 || value shr fromBits != 0) return null
            acc = (acc shl fromBits) or value
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                result.add((acc shr bits) and maxv)
            }
        }
        if (pad) {
            if (bits > 0) result.add((acc shl (toBits - bits)) and maxv)
        } else if (bits >= fromBits || (acc shl (toBits - bits)) and maxv != 0) {
            return null
        }
        return result
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun List<Int>.toByteArray(): ByteArray = ByteArray(size) { this[it].toByte() }
}
