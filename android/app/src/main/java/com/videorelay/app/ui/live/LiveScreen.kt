package com.videorelay.app.ui.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.videorelay.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    onStreamClick: (String) -> Unit,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Live Streams", fontWeight = FontWeight.Bold) },
        )

        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorScreen(
                message = uiState.error!!,
                onRetry = { viewModel.refresh() },
            )
            uiState.streams.isEmpty() -> EmptyState(
                icon = Icons.Filled.Stream,
                title = "No live streams",
                subtitle = "Check back later for live content",
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.streams, key = { it.id }) { stream ->
                        val profile = uiState.profiles[stream.pubkey]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStreamClick(stream.id) },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f),
                                ) {
                                    AsyncImage(
                                        model = stream.thumbnail,
                                        contentDescription = stream.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    // Live badge
                                    if (stream.status == "live") {
                                        Surface(
                                            color = Color.Red,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(8.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    Icons.Filled.FiberManualRecord,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(8.dp),
                                                )
                                                Text(
                                                    "LIVE",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }

                                    // Viewer count
                                    if (stream.currentParticipants > 0) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    Icons.Filled.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                                                )
                                                Text(
                                                    "${stream.currentParticipants}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                                )
                                            }
                                        }
                                    }
                                }

                                // Info
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = stream.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = profile?.picture,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = profile?.bestName ?: stream.pubkey.take(8) + "...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
