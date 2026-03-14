package com.videorelay.app.domain.model

data class ZapReceipt(
    val id: String,
    val senderPubkey: String,
    val recipientPubkey: String,
    val eventId: String,
    val amountSats: Long,
    val content: String = "",
    val createdAt: Long,
)
