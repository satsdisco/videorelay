package com.videorelay.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddRelay by remember { mutableStateOf(false) }
    var newRelayUrl by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Account section
            item {
                Text(
                    "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.loggedInPubkey != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Signed in", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = uiState.loggedInPubkey!!.take(16) + "...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Not signed in", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (uiState.isAmberAvailable)
                                            "Sign in with Amber to zap, comment, and upload"
                                        else
                                            "Install Amber (Nostr Signer) to sign in",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { /* TODO: Amber sign-in flow */ },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = uiState.isAmberAvailable,
                            ) {
                                Text(
                                    if (uiState.isAmberAvailable) "Sign in with Amber"
                                    else "Amber not installed"
                                )
                            }
                        }
                    }
                }
            }

            // Relay section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Relays",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row {
                        TextButton(onClick = { showAddRelay = !showAddRelay }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                        TextButton(onClick = { viewModel.resetRelays() }) {
                            Text("Reset")
                        }
                    }
                }
            }

            // Add relay input
            if (showAddRelay) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newRelayUrl,
                            onValueChange = { newRelayUrl = it },
                            placeholder = { Text("wss://...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.addRelay(newRelayUrl)
                                newRelayUrl = ""
                                showAddRelay = false
                            },
                            enabled = newRelayUrl.startsWith("wss://"),
                        ) {
                            Icon(Icons.Filled.Check, "Add")
                        }
                    }
                }
            }

            // Relay list
            items(uiState.relays) { relay ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = relay.enabled,
                            onCheckedChange = { viewModel.toggleRelay(relay.url) },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = relay.url.removePrefix("wss://"),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (relay.enabled)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { viewModel.removeRelay(relay.url) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // About section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "VideoRelay",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Decentralized video for Nostr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Free & open source. No tracking, no ads, no censorship.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { /* TODO: open website */ },
                                label = { Text("Website") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Language, null, modifier = Modifier.size(16.dp))
                                },
                            )
                            AssistChip(
                                onClick = { /* TODO: open GitHub */ },
                                label = { Text("Source") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                                },
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
