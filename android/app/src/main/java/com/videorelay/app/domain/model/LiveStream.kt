package com.videorelay.app.domain.model

data class LiveStream(
    val id: String,
    val pubkey: String,
    val title: String,
    val summary: String,
    val thumbnail: String,
    val streamUrl: String,
    val status: String, // "live", "ended", "planned"
    val starts: Long? = null,
    val ends: Long? = null,
    val currentParticipants: Int = 0,
    val tags: List<String> = emptyList(),
    val publishedAt: Long = 0,
)
