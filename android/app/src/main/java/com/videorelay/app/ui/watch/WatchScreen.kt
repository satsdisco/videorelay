package com.videorelay.app.ui.watch

import android.app.Activity
import android.content.Intent

import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
@Suppress("DEPRECATION")
@Composable
fun WatchScreen(
    videoId: String,
    onBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: WatchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showZapDialog by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    val activity = context as? Activity

    // Handle back press in fullscreen
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
        showSystemBars(activity)
    }

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

    // Cleanup player + restore system bars
    DisposableEffect(Unit) {
        onDispose {
            player.release()
            showSystemBars(activity)
        }
    }

    if (isFullscreen) {
        // Fullscreen mode — player takes entire screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Exit fullscreen button
            IconButton(
                onClick = {
                    isFullscreen = false
                    showSystemBars(activity)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    Icons.Filled.FullscreenExit,
                    contentDescription = "Exit fullscreen",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    } else {
        // Normal mode
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
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
                            useController = true
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Top bar overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = {
                            isFullscreen = true
                            // Don't force orientation — fill screen in current mode
                            hideSystemBars(activity)
                        },
                    ) {
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
                                color = MaterialTheme.colorScheme.onSurface,
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
                                        color = MaterialTheme.colorScheme.onSurface,
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
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Action row (zap, share, download)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                ActionButton(Icons.Filled.ElectricBolt, "⚡ ${formatZapCount(video.zapCount)}") {
                                    showZapDialog = true
                                }
                                ActionButton(Icons.Filled.Share, "Share") {
                                    val shareText = "${video.title}\n\nhttps://videorelay.lol/watch/${video.id}"
                                    val shareIntent = Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            putExtra(Intent.EXTRA_SUBJECT, video.title)
                                        },
                                        "Share video"
                                    )
                                    context.startActivity(shareIntent)
                                }
                                ActionButton(Icons.Filled.Download, "Save") {
                                    // TODO: download
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Description
                        if (video.summary.isNotBlank()) {
                            item {
                                Text(
                                    text = video.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Comments header
                        item {
                            Text(
                                text = "Comments (${uiState.comments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (uiState.comments.isEmpty()) {
                            item {
                                Text(
                                    text = "No comments yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        // Related videos
                        if (uiState.relatedVideos.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "More from this creator",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
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

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }

    // Zap dialog
    if (showZapDialog && uiState.video != null) {
        ZapDialog(
            creatorProfile = uiState.creatorProfile,
            videoId = uiState.video!!.id,
            nip57Zap = viewModel.nip57Zap,
            context = context,
            onDismiss = { showZapDialog = false },
        )
    }
}

private fun hideSystemBars(activity: Activity?) {
    activity ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        activity.window.insetsController?.let { controller ->
            controller.hide(android.view.WindowInsets.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}

private fun showSystemBars(activity: Activity?) {
    activity ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(android.view.WindowInsets.Type.systemBars())
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
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
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
