package com.videorelay.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videorelay.app.domain.model.Profile
import com.videorelay.app.domain.model.Video

@Composable
fun VideoGrid(
    videos: List<Video>,
    profiles: Map<String, Profile> = emptyMap(),
    isLoading: Boolean = false,
    isLoadingMore: Boolean = false,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Trigger load more when near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= videos.size - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isLoadingMore) {
            onLoadMore?.invoke()
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        items(videos, key = { it.id }) { video ->
            VideoCard(
                video = video,
                profile = profiles[video.pubkey],
                onClick = { onVideoClick(video.id) },
                onChannelClick = { onChannelClick(video.pubkey) },
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
