package com.videorelay.app.data.nostr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class NostrEvent(
    val id: String = "",
    val pubkey: String = "",
    val created_at: Long = 0,
    val kind: Int = 0,
    val tags: List<List<String>> = emptyList(),
    val content: String = "",
    val sig: String = "",
) {
    fun getTag(name: String): String? =
        tags.firstOrNull { it.size >= 2 && it[0] == name }?.get(1)

    fun getAllTags(name: String): List<String> =
        tags.filter { it.size >= 2 && it[0] == name }.map { it[1] }
}

@Serializable
data class UnsignedEvent(
    val kind: Int,
    val content: String,
    val tags: List<List<String>>,
    val created_at: Long = System.currentTimeMillis() / 1000,
)
