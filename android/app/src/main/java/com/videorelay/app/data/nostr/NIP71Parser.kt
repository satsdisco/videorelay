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
        val isShort = isShortKind || hasShortTag || (durationSecs in 1..60)

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
            // imeta can be ["imeta", "url https://...", "m video/mp4"] or ["imeta", "url", "https://..."]
            // Handle both formats
            var urlValue: String? = null
            var mimeValue: String? = null

            for (field in imeta.drop(1)) {
                when {
                    field.startsWith("url ") -> urlValue = field.removePrefix("url ").trim()
                    field == "url" -> {} // next element is the URL — handled below
                    field.startsWith("m ") -> mimeValue = field.removePrefix("m ").trim()
                    field.startsWith("image ") -> {
                        // Some events use "image https://..." directly in imeta
                        if (!isVideo) return field.removePrefix("image ").trim()
                    }
                }
            }

            // Handle ["imeta", "url", "https://..."] format (separate elements)
            if (urlValue == null) {
                val urlIdx = imeta.indexOf("url")
                if (urlIdx >= 0 && urlIdx + 1 < imeta.size) {
                    urlValue = imeta[urlIdx + 1].trim()
                }
            }

            if (urlValue.isNullOrBlank()) continue

            if (isVideo) {
                val isVideoUrl = urlValue.matches(Regex(""".*\.(mp4|webm|mov|m3u8|ogg)(\?.*)?$""", RegexOption.IGNORE_CASE))
                    || mimeValue?.startsWith("video/") == true
                    || urlValue.contains("video")
                if (isVideoUrl) return urlValue
            } else {
                val isImageUrl = urlValue.matches(Regex(""".*\.(jpg|jpeg|png|webp|gif)(\?.*)?$""", RegexOption.IGNORE_CASE))
                    || mimeValue?.startsWith("image/") == true
                if (isImageUrl) return urlValue
                // Also accept any URL from image-typed imeta entries
                if (mimeValue == null && !urlValue.matches(Regex(""".*\.(mp4|webm|mov|m3u8|ogg)(\?.*)?$""", RegexOption.IGNORE_CASE))) {
                    // Could be an image without extension — return it
                    return urlValue
                }
            }
        }

        // Also check "image" tag directly (some kind 21/22 events use this)
        if (!isVideo) {
            event.getTag("image")?.let { return it }
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
