package com.videorelay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video
import com.videorelay.app.ui.theme.VRPurple
import com.videorelay.app.ui.theme.VRPurpleDim

@Composable
fun VideoCard(
    video: Video,
    profile: Profile? = null,
    onClick: () -> Unit,
    onChannelClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 16.dp),
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            val thumbnailUrl = video.thumbnail.ifBlank { null }

            if (thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(200)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        // Shimmer placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    },
                    error = {
                        ThumbnailFallback(title = video.title, videoId = video.id)
                    },
                )
            } else {
                ThumbnailFallback(title = video.title, videoId = video.id)
            }

            // Duration badge
            if (video.duration.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                ) {
                    Text(
                        text = video.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info row: avatar + title + meta
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            // Creator avatar
            val avatarUrl = profile?.picture?.ifBlank { null }
            SubcomposeAsyncImage(
                model = avatarUrl?.let {
                    ImageRequest.Builder(context)
                        .data(it)
                        .crossfade(true)
                        .build()
                },
                contentDescription = profile?.bestName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (onChannelClick != null) Modifier.clickable(onClick = onChannelClick)
                        else Modifier
                    ),
                loading = {
                    AvatarFallback(name = profile?.bestName)
                },
                error = {
                    AvatarFallback(name = profile?.bestName)
                },
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(profile?.bestName ?: video.pubkey.take(8) + "...")
                        append(" · ")
                        append(timeAgo(video.publishedAt))
                        if (video.zapCount > 0) {
                            append(" · ⚡${formatZapCount(video.zapCount)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// Gradient pairs matching web app's getFallbackThumb — deterministic by ID
private val FALLBACK_GRADIENTS = listOf(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), // indigo → violet
    listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), // pink → rose
    listOf(Color(0xFFF59E0B), Color(0xFFEF4444)), // amber → red
    listOf(Color(0xFF10B981), Color(0xFF06B6D4)), // emerald → cyan
    listOf(Color(0xFF3B82F6), Color(0xFF6366F1)), // blue → indigo
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), // violet → pink
    listOf(Color(0xFF14B8A6), Color(0xFF22D3EE)), // teal → cyan
    listOf(Color(0xFFF97316), Color(0xFFEAB308)), // orange → yellow
)

@Composable
private fun ThumbnailFallback(title: String, videoId: String = "") {
    // Deterministic gradient based on ID (same as web app)
    val index = if (videoId.isNotEmpty()) videoId[0].code % FALLBACK_GRADIENTS.size else 0
    val (from, to) = FALLBACK_GRADIENTS[index].let { it[0] to it[1] }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(from, to))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = title,
            modifier = Modifier.size(48.dp),
            tint = Color.White.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun AvatarFallback(name: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (name != null && name.isNotBlank()) {
            Text(
                text = name.first().uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun timeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestamp
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        diff < 2592000 -> "${diff / 604800}w ago"
        diff < 31536000 -> "${diff / 2592000}mo ago"
        else -> "${diff / 31536000}y ago"
    }
}

fun formatZapCount(sats: Int): String = when {
    sats >= 1_000_000 -> "%.1fM".format(sats / 1_000_000f)
    sats >= 1_000 -> "%.1fK".format(sats / 1_000f)
    else -> sats.toString()
}
