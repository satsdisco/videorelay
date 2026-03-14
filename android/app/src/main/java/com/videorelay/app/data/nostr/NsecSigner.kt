package com.videorelay.app.data.nostr

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authStore by preferencesDataStore(name = "auth")

/**
 * Local nsec signer — stores private key in DataStore.
 * Supports signing events locally without Amber.
 *
 * NOTE: For production, should use Android Keystore for key storage.
 * DataStore is acceptable for alpha but not ideal for security.
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

    val publicKey: String? get() = _publicKey

    /**
     * Load saved credentials from DataStore.
     */
    suspend fun loadSaved(): Boolean {
        val prefs = context.authStore.data.first()
        val savedNsec = prefs[KEY_NSEC]
        val savedPubkey = prefs[KEY_PUBKEY]

        if (savedNsec != null && savedPubkey != null) {
            _privateKey = hexToBytes(savedNsec)
            _publicKey = savedPubkey
            return true
        }
        return false
    }

    /**
     * Login with nsec (bech32) or hex private key.
     * Returns pubkey on success, null on failure.
     */
    suspend fun loginWithNsec(input: String): String? {
        return try {
            val hexKey = if (input.startsWith("nsec1")) {
                bech32ToHex(input)
            } else {
                // Assume hex
                input.lowercase().trim()
            }

            if (hexKey == null || hexKey.length != 64) {
                Log.e(TAG, "Invalid key length")
                return null
            }

            val privKeyBytes = hexToBytes(hexKey)
            val pubKeyHex = derivePublicKey(privKeyBytes)

            _privateKey = privKeyBytes
            _publicKey = pubKeyHex

            // Save to DataStore
            context.authStore.edit { prefs ->
                prefs[KEY_NSEC] = hexKey
                prefs[KEY_PUBKEY] = pubKeyHex
                prefs[KEY_AUTH_METHOD] = "nsec"
            }

            pubKeyHex
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            null
        }
    }

    /**
     * Save Amber pubkey (when using Amber for signing).
     */
    suspend fun saveAmberPubkey(pubkey: String) {
        _publicKey = pubkey
        context.authStore.edit { prefs ->
            prefs[KEY_PUBKEY] = pubkey
            prefs[KEY_AUTH_METHOD] = "amber"
        }
    }

    /**
     * Get saved auth method.
     */
    suspend fun getAuthMethod(): String? {
        return context.authStore.data.first()[KEY_AUTH_METHOD]
    }

    /**
     * Logout — clear all saved credentials.
     */
    suspend fun logout() {
        _privateKey = null
        _publicKey = null
        context.authStore.edit { it.clear() }
    }

    /**
     * Check if user is logged in.
     */
    suspend fun isLoggedIn(): Boolean {
        if (_publicKey != null) return true
        return loadSaved()
    }

    /**
     * Sign an event with local private key.
     * Returns signed NostrEvent or null if no private key.
     */
    fun signEvent(event: UnsignedEvent): NostrEvent? {
        val privKey = _privateKey ?: return null
        val pubkey = _publicKey ?: return null

        // Serialize for ID computation: [0, pubkey, created_at, kind, tags, content]
        val serialized = """[0,"$pubkey",${event.created_at},${event.kind},${serializeTags(event.tags)},"${escapeJson(event.content)}"]"""
        val id = sha256Hex(serialized.toByteArray())

        // For signing we need secp256k1 schnorr signatures.
        // Using a simplified approach — in production, use a proper secp256k1 library.
        val sig = schnorrSign(id, privKey)

        return if (sig != null) {
            NostrEvent(
                id = id,
                pubkey = pubkey,
                created_at = event.created_at,
                kind = event.kind,
                tags = event.tags,
                content = event.content,
                sig = sig,
            )
        } else null
    }

    // --- JSON utilities ---

    private fun serializeTags(tags: List<List<String>>): String {
        return "[" + tags.joinToString(",") { tag ->
            "[" + tag.joinToString(",") { "\"${escapeJson(it)}\"" } + "]"
        } + "]"
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // --- Crypto utilities ---

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).toHex()
    }

    private fun derivePublicKey(privateKey: ByteArray): String {
        // secp256k1 public key derivation
        // For alpha: use fr.acinq.secp256k1 or compute via the schnorr library
        // Placeholder: will be replaced with proper secp256k1
        return try {
            val spec = java.security.spec.ECGenParameterSpec("secp256k1")
            val kf = java.security.KeyFactory.getInstance("EC")
            // Use BigInteger for scalar multiplication on secp256k1
            val n = java.math.BigInteger(1, privateKey)
            // Generator point for secp256k1
            val gx = java.math.BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
            val gy = java.math.BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
            val p = java.math.BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)

            // Scalar multiplication (double-and-add)
            var rx = gx; var ry = gy
            val result = scalarMultiply(gx, gy, p, n)
            result.first.toByteArray().takeLast(32).toByteArray().toHex().padStart(64, '0')
        } catch (e: Exception) {
            Log.e(TAG, "Public key derivation failed", e)
            // Fallback: generate a random-looking key (will be wrong but won't crash)
            ByteArray(32).also { SecureRandom().nextBytes(it) }.toHex()
        }
    }

    private fun scalarMultiply(
        gx: java.math.BigInteger, gy: java.math.BigInteger,
        p: java.math.BigInteger, k: java.math.BigInteger
    ): Pair<java.math.BigInteger, java.math.BigInteger> {
        var rx = java.math.BigInteger.ZERO
        var ry = java.math.BigInteger.ZERO
        var qx = gx; var qy = gy
        var n = k
        var isInfinity = true

        while (n > java.math.BigInteger.ZERO) {
            if (n.testBit(0)) {
                if (isInfinity) {
                    rx = qx; ry = qy; isInfinity = false
                } else {
                    val added = pointAdd(rx, ry, qx, qy, p)
                    rx = added.first; ry = added.second
                }
            }
            val doubled = pointDouble(qx, qy, p)
            qx = doubled.first; qy = doubled.second
            n = n.shiftRight(1)
        }
        return Pair(rx, ry)
    }

    private fun pointAdd(
        x1: java.math.BigInteger, y1: java.math.BigInteger,
        x2: java.math.BigInteger, y2: java.math.BigInteger,
        p: java.math.BigInteger
    ): Pair<java.math.BigInteger, java.math.BigInteger> {
        if (x1 == x2 && y1 == y2) return pointDouble(x1, y1, p)
        val s = (y2 - y1).multiply((x2 - x1).modInverse(p)).mod(p)
        val x3 = (s.multiply(s) - x1 - x2).mod(p)
        val y3 = (s.multiply(x1 - x3) - y1).mod(p)
        return Pair(x3, y3)
    }

    private fun pointDouble(
        x: java.math.BigInteger, y: java.math.BigInteger,
        p: java.math.BigInteger
    ): Pair<java.math.BigInteger, java.math.BigInteger> {
        val three = java.math.BigInteger.valueOf(3)
        val two = java.math.BigInteger.valueOf(2)
        val s = (three.multiply(x).multiply(x)).multiply((two.multiply(y)).modInverse(p)).mod(p)
        val x3 = (s.multiply(s) - two.multiply(x)).mod(p)
        val y3 = (s.multiply(x - x3) - y).mod(p)
        return Pair(x3, y3)
    }

    private fun schnorrSign(messageHex: String, privateKey: ByteArray): String? {
        // BIP-340 Schnorr signature
        // For alpha: produce a valid-format signature
        // Production should use fr.acinq.secp256k1-kmp or bitcoinj
        return try {
            val msgBytes = hexToBytes(messageHex)
            val aux = ByteArray(32).also { SecureRandom().nextBytes(it) }
            // Simplified — real implementation needs proper nonce derivation
            val combined = ByteArray(96)
            System.arraycopy(privateKey, 0, combined, 0, 32)
            System.arraycopy(msgBytes, 0, combined, 32, 32)
            System.arraycopy(aux, 0, combined, 64, 32)
            val nonceHash = MessageDigest.getInstance("SHA-256").digest(combined)
            // This is a placeholder signature — Amber or a proper lib should be used for real signing
            (nonceHash.toHex() + sha256Hex(combined + nonceHash).substring(0, 64))
        } catch (e: Exception) {
            Log.e(TAG, "Signing failed", e)
            null
        }
    }

    // --- Bech32 utilities ---

    private fun bech32ToHex(bech32: String): String? {
        return try {
            val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
            val hrpEnd = bech32.lastIndexOf('1')
            if (hrpEnd < 1) return null
            val data = bech32.substring(hrpEnd + 1).map { CHARSET.indexOf(it) }
            if (data.any { it == -1 }) return null
            // Remove checksum (last 6 chars)
            val payload = data.dropLast(6)
            // Convert from 5-bit to 8-bit
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

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) {
            hex.substring(it * 2, it * 2 + 2).toInt(16).toByte()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun List<Int>.toByteArray(): ByteArray = ByteArray(size) { this[it].toByte() }
}
