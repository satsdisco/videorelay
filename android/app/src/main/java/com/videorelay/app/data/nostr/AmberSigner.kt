package com.videorelay.app.data.nostr

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NIP-55 signer using Amber (or any Android Nostr Signer).
 * Uses the nostrsigner: URI scheme as specified in NIP-55.
 *
 * Flow:
 * 1. Create intent with nostrsigner: scheme
 * 2. Launch with ActivityResultLauncher
 * 3. Read result from intent extras
 */
@Singleton
class AmberSigner @Inject constructor(
    @ApplicationContext private val context: Context,
) : NostrSigner {

    companion object {
        const val AMBER_PACKAGE = "com.greenart7c3.nostrsigner"
    }

    private var _publicKey: String? = null
    private var _signerPackage: String? = null

    override val publicKey: String? get() = _publicKey

    /**
     * Check if any NIP-55 signer is installed by querying the nostrsigner: scheme.
     */
    override suspend fun isAvailable(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:"))
            val infos = context.packageManager.queryIntentActivities(intent, 0)
            infos.size > 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Create the NIP-55 get_public_key intent.
     * Launch this with rememberLauncherForActivityResult.
     */
    fun getPublicKeyIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:")).apply {
            putExtra("type", "get_public_key")
            // Request permissions for signing video-related events
            putExtra("permissions", """[{"type":"sign_event","kind":1},{"type":"sign_event","kind":7},{"type":"sign_event","kind":9734},{"type":"sign_event","kind":1111}]""")
        }
    }

    /**
     * Create a NIP-55 sign_event intent.
     */
    fun getSignEventIntent(eventJson: String, eventId: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:$eventJson")).apply {
            `package` = _signerPackage ?: AMBER_PACKAGE
            putExtra("type", "sign_event")
            putExtra("id", eventId)
            _publicKey?.let { putExtra("current_user", it) }
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * Handle the result from get_public_key.
     * Call this from the ActivityResult callback.
     */
    fun handleGetPublicKeyResult(resultIntent: Intent?): String? {
        val pubkey = resultIntent?.getStringExtra("result") ?: return null
        val signerPackage = resultIntent.getStringExtra("package")
        _publicKey = pubkey
        _signerPackage = signerPackage ?: AMBER_PACKAGE
        return pubkey
    }

    fun setPublicKey(pubkey: String) {
        _publicKey = pubkey
    }

    override suspend fun sign(event: UnsignedEvent): NostrEvent? {
        // NIP-55 signing is intent-based — handled by Activity
        return null
    }
}
