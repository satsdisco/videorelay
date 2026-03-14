package com.videorelay.app.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.videorelay.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onVideoClick: (String) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Downloads", fontWeight = FontWeight.Bold) },
        )

        if (downloads.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.DownloadDone,
                title = "No downloads",
                subtitle = "Videos you save for offline viewing will appear here",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(downloads, key = { it.videoId }) { download ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (download.status == "complete") {
                                    onVideoClick(download.videoId)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(width = 120.dp, height = 68.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            ) {
                                AsyncImage(
                                    model = download.thumbnail,
                                    contentDescription = download.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )

                                // Status overlay
                                when (download.status) {
                                    "downloading" -> {
                                        CircularProgressIndicator(
                                            progress = { download.progress / 100f },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .align(Alignment.Center),
                                            strokeWidth = 3.dp,
                                        )
                                    }
                                    "error" -> {
                                        Icon(
                                            Icons.Filled.Error,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .align(Alignment.Center),
                                        )
                                    }
                                    "complete" -> {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = download.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (download.status) {
                                        "pending" -> "Waiting..."
                                        "downloading" -> "${download.progress}%"
                                        "complete" -> "%.1f MB".format(download.fileSize / (1024.0 * 1024.0))
                                        "error" -> "Download failed"
                                        else -> download.status
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (download.status) {
                                        "error" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }

                            // Delete button
                            IconButton(onClick = { viewModel.deleteDownload(download) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Progress bar for active downloads
                        if (download.status == "downloading") {
                            LinearProgressIndicator(
                                progress = { download.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
