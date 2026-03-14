package com.videorelay.app.data.nostr

import kotlinx.serialization.json.*

/**
 * Build nostr REQ filter objects.
 */
data class NostrFilter(
    val kinds: List<Int>? = null,
    val authors: List<String>? = null,
    val ids: List<String>? = null,
    val since: Long? = null,
    val until: Long? = null,
    val limit: Int? = null,
    val tags: Map<String, List<String>> = emptyMap(), // e.g. "#t" -> ["bitcoin"]
    val search: String? = null,
) {
    fun toJson(): JsonObject = buildJsonObject {
        kinds?.let { put("kinds", JsonArray(it.map { k -> JsonPrimitive(k) })) }
        authors?.let { put("authors", JsonArray(it.map { a -> JsonPrimitive(a) })) }
        ids?.let { put("ids", JsonArray(it.map { i -> JsonPrimitive(i) })) }
        since?.let { put("since", JsonPrimitive(it)) }
        until?.let { put("until", JsonPrimitive(it)) }
        limit?.let { put("limit", JsonPrimitive(it)) }
        tags.forEach { (key, values) ->
            put(key, JsonArray(values.map { v -> JsonPrimitive(v) }))
        }
        search?.let { put("search", JsonPrimitive(it)) }
    }
}
