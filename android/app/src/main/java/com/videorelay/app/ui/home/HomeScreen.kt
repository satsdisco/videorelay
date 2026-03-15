package com.videorelay.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
    val pullRefreshState = rememberPullToRefreshState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    text = buildAnnotatedString {
                        append("Video")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("Relay")
                        }
                    },
                    fontWeight = FontWeight.Bold,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
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
            containerColor = MaterialTheme.colorScheme.background,
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

        // Time period filter — only for Trending + Most Zapped
        if (uiState.selectedTab == FeedTab.Trending || uiState.selectedTab == FeedTab.MostZapped) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TimePeriod.entries) { period ->
                    FilterChip(
                        selected = uiState.timePeriod == period,
                        onClick = { viewModel.selectTimePeriod(period) },
                        label = { Text(period.label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        } else {
            // Category chips for Home/Following
            CategoryBar(
                selectedTag = uiState.selectedTag,
                onTagSelected = { viewModel.selectTag(it) },
            )
        }

        // Content with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = uiState.isLoading && uiState.videos.isNotEmpty(),
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize(),
        ) {
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
}
