package com.videorelay.app.data.nostr

import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NIP-57 Zap flow:
 * 1. Fetch recipient's profile → get lud16/lud06
 * 2. Resolve LNURL pay endpoint
 * 3. Create kind 9734 zap request
 * 4. Request bolt11 invoice from LNURL callback
 * 5. Open in Lightning wallet via intent
 */
@Singleton
class NIP57Zap @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    /**
     * Resolve a Lightning address (user@domain) to an LNURL-pay endpoint URL.
     */
    fun resolveLud16(lud16: String): String? {
        val parts = lud16.split("@")
        if (parts.size != 2) return null
        return "https://${parts[1]}/.well-known/lnurlp/${parts[0]}"
    }

    /**
     * Fetch LNURL-pay params from a pay URL.
     */
    suspend fun fetchLnurlPayParams(payUrl: String): LnurlPayParams? {
        return try {
            val request = Request.Builder().url(payUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val json = Json.parseToJsonElement(response.body?.string() ?: "").jsonObject
            LnurlPayParams(
                callback = json["callback"]?.jsonPrimitive?.content ?: payUrl,
                minSendable = json["minSendable"]?.jsonPrimitive?.longOrNull ?: 1000,
                maxSendable = json["maxSendable"]?.jsonPrimitive?.longOrNull ?: 1_000_000_000,
                allowsNostr = json["allowsNostr"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build an unsigned kind 9734 zap request event.
     */
    fun buildZapRequest(
        recipientPubkey: String,
        eventId: String,
        amountMsat: Long,
        relays: List<String>,
        content: String = "",
    ): UnsignedEvent {
        return UnsignedEvent(
            kind = NostrConstants.ZAP_REQUEST_KIND,
            content = content,
            tags = listOf(
                listOf("p", recipientPubkey),
                listOf("e", eventId),
                listOf("amount", amountMsat.toString()),
                listOf("relays") + relays.take(5),
            ),
        )
    }

    /**
     * Request a bolt11 invoice from an LNURL callback with the signed zap request.
     */
    suspend fun requestInvoice(
        callback: String,
        amountMsat: Long,
        signedZapRequest: NostrEvent,
    ): String? {
        return try {
            val zapJson = Json.encodeToString(NostrEvent.serializer(), signedZapRequest)
            val separator = if ("?" in callback) "&" else "?"
            val url = "${callback}${separator}amount=${amountMsat}&nostr=${
                java.net.URLEncoder.encode(zapJson, "UTF-8")
            }"

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val json = Json.parseToJsonElement(response.body?.string() ?: "").jsonObject
            json["pr"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}

data class LnurlPayParams(
    val callback: String,
    val minSendable: Long,
    val maxSendable: Long,
    val allowsNostr: Boolean,
)
