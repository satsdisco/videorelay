package com.videorelay.app.domain.model

data class Comment(
    val id: String,
    val pubkey: String,
    val content: String,
    val publishedAt: Long,
    val replyTo: String? = null, // parent comment id
)
