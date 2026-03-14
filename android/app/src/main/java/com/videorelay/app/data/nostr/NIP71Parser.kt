package com.videorelay.app.data.nostr

import com.videorelay.app.domain.model.Video

/**
 * Parse NIP-71 video events into domain Video objects.
 * Ported from web app's parseVideoEvent().
 */
object NIP71Parser {

    fun parse(event: NostrEvent): Video? {
        // For kind 1 (text notes), check if content has video URLs
        if (event.kind == NostrConstants.TEXT_NOTE_KIND) {
            val hasVideoUrl = NostrConstants.VIDEO_URL_PATTERN.containsMatchIn(event.content)
            val hasVideoTag = event.getTag("url")?.let {
                it.matches(Regex(""".*\.(mp4|webm|mov|m3u8|ogg)(\?.*)?$""", RegexOption.IGNORE_CASE))
            } ?: false
            if (!hasVideoUrl && !hasVideoTag) return null
        }

        // Extract video URL from tags
        var videoUrl = event.getTag("url") ?: ""

        // Check imeta tags
        if (videoUrl.isBlank()) {
            videoUrl = extractFromImeta(event, isVideo = true) ?: ""
        }

        // Check streaming/recording tags
        if (videoUrl.isBlank()) {
            videoUrl = event.getTag("streaming") ?: event.getTag("recording") ?: ""
        }

        // For kind 1: extract from content
        if (videoUrl.isBlank() && event.kind == NostrConstants.TEXT_NOTE_KIND) {
            videoUrl = NostrConstants.VIDEO_URL_PATTERN.find(event.content)?.value ?: ""
        }

        if (videoUrl.isBlank()) return null

        // Extract thumbnail
        var thumbnail = event.getTag("thumb")
            ?: event.getTag("image")
            ?: event.getTag("thumbnail")
            ?: ""

        if (thumbnail.isBlank()) {
            thumbnail = extractFromImeta(event, isVideo = false) ?: ""
        }

        if (thumbnail.isBlank() && event.kind == NostrConstants.TEXT_NOTE_KIND) {
            thumbnail = NostrConstants.IMAGE_URL_PATTERN.find(event.content)?.value ?: ""
        }

        // Extract title
        var title = event.getTag("title") ?: event.getTag("subject") ?: ""
        if (title.isBlank() && event.kind == NostrConstants.TEXT_NOTE_KIND) {
            title = event.content.lineSequence().firstOrNull()
                ?.replace(NostrConstants.VIDEO_URL_PATTERN, "")
                ?.replace(NostrConstants.IMAGE_URL_PATTERN, "")
                ?.replace(Regex("""https?://\S+"""), "")
                ?.trim()
                ?.ifBlank { "Untitled Video" }
                ?: "Untitled Video"
        }
        if (title.isBlank()) title = "Untitled Video"

        val durationSecs = event.getTag("duration")?.toIntOrNull() ?: 0
        val hashtags = event.getAllTags("t")

        val isShortKind = event.kind == NostrConstants.SHORT_VIDEO_KIND ||
                event.kind == NostrConstants.ADDRESSABLE_SHORT_KIND
        val hasShortTag = hashtags.any { it.lowercase() in NostrConstants.SHORT_TAGS }
        val isShort = isShortKind || hasShortTag || (durationSecs in 1..30)

        return Video(
            id = event.id,
            pubkey = event.pubkey,
            title = title,
            summary = event.content.ifBlank { event.getTag("summary") ?: "" },
            thumbnail = thumbnail,
            videoUrl = videoUrl,
            duration = formatDuration(durationSecs),
            durationSeconds = durationSecs,
            publishedAt = event.created_at,
            tags = hashtags,
            isShort = isShort,
            kind = event.kind,
        )
    }

    private fun extractFromImeta(event: NostrEvent, isVideo: Boolean): String? {
        val imetaTags = event.tags.filter { it.firstOrNull() == "imeta" }
        for (imeta in imetaTags) {
            for (field in imeta.drop(1)) {
                if (field.startsWith("url ")) {
                    val url = field.removePrefix("url ")
                    if (isVideo) {
                        if (url.matches(Regex(""".*\.(mp4|webm|mov|m3u8|ogg)(\?.*)?$""", RegexOption.IGNORE_CASE)) ||
                            url.contains("video")
                        ) return url
                    } else {
                        if (url.matches(Regex(""".*\.(jpg|jpeg|png|webp|gif)(\?.*)?$""", RegexOption.IGNORE_CASE))) {
                            return url
                        }
                    }
                }
            }
        }
        return null
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            "%d:%02d:%02d".format(h, m, s)
        } else {
            "%d:%02d".format(m, s)
        }
    }
}
