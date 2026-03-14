package com.videorelay.app.ui.channel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.videorelay.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    pubkey: String,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(pubkey) {
        viewModel.loadChannel(pubkey)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(uiState.profile?.bestName ?: "Channel") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
        )

        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorScreen(message = uiState.error!!)
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                ) {
                    // Profile header
                    item {
                        val profile = uiState.profile
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Banner
                            if (profile?.banner?.isNotBlank() == true) {
                                AsyncImage(
                                    model = profile.banner,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                )
                                Spacer(modifier = Modifier.height((-40).dp))
                            }

                            // Avatar
                            AsyncImage(
                                model = profile?.picture,
                                contentDescription = profile?.bestName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = profile?.bestName ?: pubkey.take(12) + "...",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            profile?.nip05?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            profile?.about?.takeIf { it.isNotBlank() }?.let {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${uiState.videos.size} videos",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Start),
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Videos
                    items(uiState.videos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            profile = uiState.profile,
                            onClick = { onVideoClick(video.id) },
                        )
                    }

                    if (uiState.videos.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.Info,
                                title = "No videos yet",
                                subtitle = "This creator hasn't published any videos",
                            )
                        }
                    }
                }
            }
        }
    }
}
