package com.videorelay.app.data.nostr

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages WebSocket connections to multiple nostr relays.
 * Sends subscriptions, receives events, deduplicates.
 */
@Singleton
class RelayPool @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    companion object {
        private const val TAG = "RelayPool"
        private const val TIMEOUT_MS = 4000L  // 4s timeout — don't let slow relays block the UI
    }

    private val connections = ConcurrentHashMap<String, WebSocket>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Query multiple relays with a filter, collect events, deduplicate.
     * Each relay is queried independently with a timeout so slow relays don't block.
     */
    suspend fun query(
        relays: List<String>,
        filter: NostrFilter,
        timeoutMs: Long = TIMEOUT_MS,
    ): List<NostrEvent> = coroutineScope {
        val seen = ConcurrentHashMap.newKeySet<String>()
        val events = ConcurrentHashMap.newKeySet<NostrEvent>()

        val jobs = relays.map { relayUrl ->
            async {
                try {
                    withTimeout(timeoutMs) {
                        queryRelay(relayUrl, filter).forEach { event ->
                            if (seen.add(event.id)) {
                                events.add(event)
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Timeout querying $relayUrl")
                } catch (e: Exception) {
                    Log.w(TAG, "Error querying $relayUrl: ${e.message}")
                }
            }
        }

        jobs.awaitAll()
        events.toList()
    }

    /**
     * Query a single relay synchronously via a short-lived WebSocket.
     */
    private suspend fun queryRelay(
        relayUrl: String,
        filter: NostrFilter,
    ): List<NostrEvent> = suspendCancellableCoroutine { cont ->
        val events = mutableListOf<NostrEvent>()
        val subId = "vr_${System.nanoTime()}"

        val request = Request.Builder()
            .url(relayUrl)
            .build()

        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send REQ
                val req = buildJsonArray {
                    add(JsonPrimitive("REQ"))
                    add(JsonPrimitive(subId))
                    add(filter.toJson())
                }
                webSocket.send(req.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = Json.parseToJsonElement(text).jsonArray
                    val type = msg[0].jsonPrimitive.content

                    when (type) {
                        "EVENT" -> {
                            val eventJson = msg[2].jsonObject
                            val event = Json.decodeFromJsonElement<NostrEvent>(eventJson)
                            events.add(event)
                        }
                        "EOSE" -> {
                            // End of stored events — close and return
                            webSocket.close(1000, "done")
                            if (cont.isActive) cont.resumeWith(Result.success(events))
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Parse error from $relayUrl: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (cont.isActive) cont.resumeWith(Result.success(events))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (cont.isActive) cont.resumeWith(Result.success(events))
            }
        })

        cont.invokeOnCancellation {
            ws.cancel()
        }
    }

    /**
     * Publish an event to multiple relays.
     */
    suspend fun publish(relays: List<String>, event: NostrEvent) = coroutineScope {
        relays.map { relayUrl ->
            async {
                try {
                    publishToRelay(relayUrl, event)
                } catch (e: Exception) {
                    Log.w(TAG, "Publish failed to $relayUrl: ${e.message}")
                }
            }
        }.awaitAll()
    }

    private suspend fun publishToRelay(relayUrl: String, event: NostrEvent) =
        suspendCancellableCoroutine { cont ->
            val request = Request.Builder().url(relayUrl).build()
            val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val eventMsg = buildJsonArray {
                        add(JsonPrimitive("EVENT"))
                        add(Json.encodeToJsonElement(event))
                    }
                    webSocket.send(eventMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val msg = Json.parseToJsonElement(text).jsonArray
                        if (msg[0].jsonPrimitive.content == "OK") {
                            webSocket.close(1000, "published")
                            if (cont.isActive) cont.resumeWith(Result.success(Unit))
                        }
                    } catch (_: Exception) {}
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (cont.isActive) cont.resumeWith(Result.success(Unit))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (cont.isActive) cont.resumeWith(Result.success(Unit))
                }
            })

            cont.invokeOnCancellation { ws.cancel() }
        }

    fun close() {
        connections.values.forEach { it.cancel() }
        connections.clear()
        scope.cancel()
    }
}
