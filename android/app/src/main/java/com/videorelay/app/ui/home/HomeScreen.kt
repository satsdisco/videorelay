package com.videorelay.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videorelay.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onVideoClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = "VideoRelay",
                    fontWeight = FontWeight.Bold,
                )
            },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            },
        )

        // Feed tabs
        ScrollableTabRow(
            selectedTabIndex = FeedTab.entries.indexOf(uiState.selectedTab),
            edgePadding = 16.dp,
            divider = {},
        ) {
            FeedTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                FeedTab.Home -> "Home"
                                FeedTab.Trending -> "Trending"
                                FeedTab.MostZapped -> "⚡ Most Zapped"
                                FeedTab.Following -> "Following"
                            },
                        )
                    },
                )
            }
        }

        // Category chips
        CategoryBar(
            selectedTag = uiState.selectedTag,
            onTagSelected = { viewModel.selectTag(it) },
        )

        // Content
        when {
            uiState.isLoading && uiState.videos.isEmpty() -> {
                LoadingScreen()
            }
            uiState.error != null && uiState.videos.isEmpty() -> {
                ErrorScreen(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                )
            }
            else -> {
                VideoGrid(
                    videos = uiState.videos,
                    profiles = uiState.profiles,
                    isLoading = uiState.isLoading,
                    isLoadingMore = uiState.isLoadingMore,
                    onVideoClick = onVideoClick,
                    onChannelClick = onChannelClick,
                    onLoadMore = { viewModel.loadMore() },
                )
            }
        }
    }
}
