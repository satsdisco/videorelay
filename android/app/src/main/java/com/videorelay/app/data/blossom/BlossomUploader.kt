package com.videorelay.app.data.blossom

import com.videorelay.app.data.nostr.NostrConstants
import com.videorelay.app.data.nostr.NostrEvent
import com.videorelay.app.data.nostr.NostrSigner
import com.videorelay.app.data.nostr.UnsignedEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class BlossomResult(
    val server: String,
    val url: String,
    val success: Boolean,
    val error: String? = null,
)

@Singleton
class BlossomUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    // Upload client with longer timeouts for video files
    private val uploadClient by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    /**
     * Upload a video file to all Blossom servers.
     * Returns the first successful URL.
     */
    suspend fun upload(
        file: File,
        mimeType: String,
        signedAuthEvent: NostrEvent? = null,
    ): Pair<String?, List<BlossomResult>> = coroutineScope {
        val hash = sha256(file)

        val results = NostrConstants.BLOSSOM_SERVERS.map { server ->
            async {
                uploadToServer(file, mimeType, hash, server, signedAuthEvent)
            }
        }.awaitAll()

        val firstSuccess = results.firstOrNull { it.success }

        // Log all results for debugging
        results.forEach { r ->
            android.util.Log.d("BlossomUploader", "${r.server}: success=${r.success}, error=${r.error}")
        }

        firstSuccess?.url to results
    }

    private fun uploadToServer(
        file: File,
        mimeType: String,
        hash: String,
        server: String,
        authEvent: NostrEvent?,
    ): BlossomResult {
        return try {
            val requestBuilder = Request.Builder()
                .url("$server/upload")
                .put(file.asRequestBody(mimeType.toMediaType()))

            authEvent?.let {
                val authJson = kotlinx.serialization.json.Json.encodeToString(
                    NostrEvent.serializer(), it
                )
                val encoded = android.util.Base64.encodeToString(
                    authJson.toByteArray(), android.util.Base64.NO_WRAP
                )
                requestBuilder.addHeader("Authorization", "Nostr $encoded")
            }

            val response = uploadClient.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = kotlinx.serialization.json.Json.parseToJsonElement(body)
                val url = json.jsonObject["url"]?.jsonPrimitive?.content ?: "$server/$hash"
                BlossomResult(server, url, true)
            } else {
                val errorBody = try { response.body?.string()?.take(200) } catch (_: Exception) { null }
                BlossomResult(server, "", false, "HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            BlossomResult(server, "", false, e.message)
        }
    }

    fun sha256File(file: File): String = sha256(file)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Build an unsigned Blossom auth event (kind 24242).
     */
    fun buildAuthEvent(fileHash: String): UnsignedEvent {
        return UnsignedEvent(
            kind = NostrConstants.BLOSSOM_AUTH_KIND,
            content = "Upload $fileHash",
            tags = listOf(
                listOf("t", "upload"),
                listOf("x", fileHash),
                listOf("expiration", ((System.currentTimeMillis() / 1000) + 300).toString()),
            ),
        )
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject
    get() = this as kotlinx.serialization.json.JsonObject

private val kotlinx.serialization.json.JsonElement.jsonPrimitive
    get() = this as kotlinx.serialization.json.JsonPrimitive
