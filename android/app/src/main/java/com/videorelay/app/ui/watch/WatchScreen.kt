package com.videorelay.app.ui.watch

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.videorelay.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    videoId: String,
    onBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: WatchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Initialize ExoPlayer
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    // Set media when video loads
    LaunchedEffect(uiState.video?.videoUrl) {
        uiState.video?.videoUrl?.let { url ->
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }

    // Load video data
    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }

    // Cleanup player
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Video Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Back button overlay
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when {
            uiState.isLoading -> {
                LoadingScreen()
            }
            uiState.error != null -> {
                ErrorScreen(message = uiState.error!!)
            }
            uiState.video != null -> {
                val video = uiState.video!!
                val profile = uiState.creatorProfile

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    // Video info
                    item {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${timeAgo(video.publishedAt)} · ${video.tags.joinToString(", ") { "#$it" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Creator row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChannelClick(video.pubkey) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = profile?.picture,
                                contentDescription = profile?.bestName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.bestName ?: video.pubkey.take(12) + "...",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                profile?.nip05?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Action row (zap, share, download)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            ActionButton(Icons.Filled.ElectricBolt, "⚡ ${formatZapCount(video.zapCount)}") {
                                // TODO: zap flow
                            }
                            ActionButton(Icons.Filled.Share, "Share") {
                                // TODO: share intent
                            }
                            ActionButton(Icons.Filled.Download, "Save") {
                                // TODO: download
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Comments header
                    item {
                        Text(
                            text = "Comments (${uiState.comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Comments
                    items(uiState.comments) { comment ->
                        val commentProfile = uiState.commentProfiles[comment.pubkey]
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            AsyncImage(
                                model = commentProfile?.picture,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${commentProfile?.bestName ?: comment.pubkey.take(8)} · ${timeAgo(comment.publishedAt)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    // Related videos header
                    if (uiState.relatedVideos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "More from this creator",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(uiState.relatedVideos) { relatedVideo ->
                            VideoCard(
                                video = relatedVideo,
                                profile = uiState.creatorProfile,
                                onClick = { /* TODO: navigate to video */ },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
