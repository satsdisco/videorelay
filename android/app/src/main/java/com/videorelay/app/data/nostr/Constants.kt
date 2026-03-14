package com.videorelay.app.data.nostr

object NostrConstants {
    val DEFAULT_RELAYS = listOf(
        "wss://relay.nostr.band",
        "wss://relay.damus.io",
        "wss://relay.primal.net",
        "wss://relay.flare.pub",
        "wss://video.nostr.build",
        "wss://nos.lol",
        "wss://relay.snort.social",
        "wss://nostr.wine",
        "wss://purplepag.es",
        "wss://offchain.pub",
    )

    const val VIDEO_KIND = 21
    const val SHORT_VIDEO_KIND = 22
    const val ADDRESSABLE_VIDEO_KIND = 34235
    const val ADDRESSABLE_SHORT_KIND = 34236
    const val TEXT_NOTE_KIND = 1
    const val PROFILE_KIND = 0
    const val REACTION_KIND = 7
    const val ZAP_REQUEST_KIND = 9734
    const val ZAP_RECEIPT_KIND = 9735
    const val COMMENT_KIND = 1111
    const val LIVE_EVENT_KIND = 30311
    const val LIVE_CHAT_KIND = 1311
    const val FOLLOW_LIST_KIND = 3
    const val BLOSSOM_AUTH_KIND = 24242

    val ALL_VIDEO_KINDS = listOf(
        TEXT_NOTE_KIND,
        VIDEO_KIND,
        SHORT_VIDEO_KIND,
        ADDRESSABLE_VIDEO_KIND,
        ADDRESSABLE_SHORT_KIND,
    )

    val BLOSSOM_SERVERS = listOf(
        "https://blossom.primal.net",
        "https://blossom.band",
        "https://24242.io",
    )

    val VIDEO_URL_PATTERN = Regex(
        """https?://[^\s"'<>]+\.(mp4|webm|mov|m3u8|ogg)(\?[^\s"'<>]*)?""",
        RegexOption.IGNORE_CASE,
    )

    val IMAGE_URL_PATTERN = Regex(
        """https?://[^\s"'<>]+\.(jpg|jpeg|png|webp|gif)(\?[^\s"'<>]*)?""",
        RegexOption.IGNORE_CASE,
    )

    val SHORT_TAGS = setOf("shorts", "short", "clip", "clips", "reel", "reels", "vertical")

    val CATEGORY_TAGS = listOf(
        "bitcoin", "lightning", "nostr", "privacy", "freedom",
        "technology", "education", "music", "gaming", "podcast",
        "news", "comedy", "art", "science", "sports",
    )
}
