package com.videorelay.app.domain.model

data class Video(
    val id: String,
    val pubkey: String,
    val title: String,
    val summary: String,
    val thumbnail: String,
    val videoUrl: String,
    val duration: String,
    val durationSeconds: Int,
    val publishedAt: Long,
    val tags: List<String>,
    val zapCount: Int = 0,
    val isShort: Boolean = false,
    val kind: Int = 21,
)
