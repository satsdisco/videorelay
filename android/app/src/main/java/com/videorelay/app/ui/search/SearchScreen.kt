package com.videorelay.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.videorelay.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            title = {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = { Text("Search videos...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.submitSearch() }
                    ),
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                )
            },
        )

        when {
            uiState.isSearching -> {
                LoadingScreen()
            }
            uiState.hasSearched && uiState.results.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No results",
                    subtitle = "Try different keywords or check your relay connections",
                )
            }
            uiState.results.isNotEmpty() -> {
                VideoGrid(
                    videos = uiState.results,
                    profiles = uiState.profiles,
                    onVideoClick = onVideoClick,
                    onChannelClick = onChannelClick,
                )
            }
            else -> {
                // Initial state — show search suggestions
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Popular searches",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOf("bitcoin", "lightning network", "nostr", "privacy", "self custody").forEach { suggestion ->
                        SuggestionChip(
                            onClick = { viewModel.updateQuery(suggestion); viewModel.submitSearch() },
                            label = { Text(suggestion) },
                        )
                    }
                }
            }
        }
    }
}
