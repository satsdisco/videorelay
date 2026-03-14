package com.videorelay.app.ui.shorts

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.videorelay.app.ui.components.LoadingScreen
import com.videorelay.app.ui.components.formatZapCount
import com.videorelay.app.ui.components.timeAgo
import com.videorelay.app.ui.theme.VRPurple

@Composable
fun ShortsScreen(
    onChannelClick: (String) -> Unit,
    viewModel: ShortsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    if (uiState.shorts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No shorts found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.refresh() }) {
                    Text("Refresh")
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { uiState.shorts.size })

    // Track current page
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentIndex(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val short = uiState.shorts[page]
            val profile = uiState.profiles[short.pubkey]
            val isCurrentPage = pagerState.currentPage == page

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Video player
                val player = remember {
                    ExoPlayer.Builder(context).build().apply {
                        repeatMode = Player.REPEAT_MODE_ONE
                        volume = 1f
                    }
                }

                LaunchedEffect(short.videoUrl, isCurrentPage) {
                    if (isCurrentPage) {
                        player.setMediaItem(MediaItem.fromUri(short.videoUrl))
                        player.prepare()
                        player.playWhenReady = true
                    } else {
                        player.pause()
                    }
                }

                DisposableEffect(Unit) {
                    onDispose { player.release() }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Bottom gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            )
                        ),
                )

                // Bottom info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .padding(bottom = 16.dp),
                ) {
                    // Creator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onChannelClick(short.pubkey) },
                    ) {
                        SubcomposeAsyncImage(
                            model = profile?.picture?.ifBlank { null }?.let {
                                ImageRequest.Builder(context).data(it).crossfade(true).build()
                            },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(VRPurple.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = (profile?.bestName ?: "?").first().uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                    )
                                }
                            },
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(VRPurple.copy(alpha = 0.3f)),
                                )
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = profile?.bestName ?: short.pubkey.take(8) + "...",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = short.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (short.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = short.tags.take(3).joinToString(" ") { "#$it" },
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                // Right-side action buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    ShortActionButton(Icons.Filled.ElectricBolt, formatZapCount(short.zapCount)) {}
                    ShortActionButton(Icons.Filled.ChatBubble, "") {}
                    ShortActionButton(Icons.Filled.Share, "Share") {}
                    ShortActionButton(Icons.Filled.Refresh, "New") {
                        viewModel.refresh()
                    }
                }
            }
        }

        // Refresh indicator
        if (uiState.isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
                    .size(24.dp),
                strokeWidth = 2.dp,
                color = VRPurple,
            )
        }
    }
}

@Composable
private fun ShortActionButton(
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
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
