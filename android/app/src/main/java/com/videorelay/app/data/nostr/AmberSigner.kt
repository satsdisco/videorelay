package com.videorelay.app.data.nostr

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NIP-55 signer using Amber (Android Nostr Signer).
 * Communicates via Android intents.
 *
 * Amber package: com.greenart7c3.nostrsigner
 */
@Singleton
class AmberSigner @Inject constructor(
    @ApplicationContext private val context: Context,
) : NostrSigner {

    companion object {
        const val AMBER_PACKAGE = "com.greenart7c3.nostrsigner"
        const val ACTION_SIGN_EVENT = "com.greenart7c3.nostrsigner.SIGN_EVENT"
        const val ACTION_GET_PUBLIC_KEY = "com.greenart7c3.nostrsigner.GET_PUBLIC_KEY"
    }

    private var _publicKey: String? = null
    override val publicKey: String? get() = _publicKey

    override suspend fun isAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo(AMBER_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Create an intent to request the user's public key from Amber.
     * The calling activity should launch this with startActivityForResult.
     */
    fun getPublicKeyIntent(): Intent {
        return Intent(ACTION_GET_PUBLIC_KEY).apply {
            `package` = AMBER_PACKAGE
        }
    }

    /**
     * Create an intent to sign an event with Amber.
     * The calling activity should launch this with startActivityForResult.
     */
    fun getSignEventIntent(eventJson: String): Intent {
        return Intent(ACTION_SIGN_EVENT).apply {
            `package` = AMBER_PACKAGE
            putExtra("event", eventJson)
        }
    }

    fun setPublicKey(pubkey: String) {
        _publicKey = pubkey
    }

    override suspend fun sign(event: UnsignedEvent): NostrEvent? {
        // Amber signing is intent-based and handled by the Activity.
        // This method is a placeholder — real signing goes through getSignEventIntent().
        return null
    }
}
