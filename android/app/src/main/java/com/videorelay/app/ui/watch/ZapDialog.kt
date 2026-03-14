package com.videorelay.app.ui.watch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videorelay.app.data.nostr.NIP57Zap
import com.videorelay.app.domain.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ZapDialog(
    creatorProfile: Profile?,
    videoId: String,
    nip57Zap: NIP57Zap,
    context: Context,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedAmount by remember { mutableIntStateOf(1000) }
    var zapComment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val amounts = listOf(21, 100, 500, 1000, 5000, 10000)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "⚡ Zap ${creatorProfile?.bestName ?: "creator"}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                // Amount selector
                Text(
                    "Amount (sats)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Amount chips in a flow layout
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        amounts.take(3).forEach { amount ->
                            FilterChip(
                                selected = selectedAmount == amount,
                                onClick = { selectedAmount = amount },
                                label = {
                                    Text(
                                        if (amount >= 1000) "${amount / 1000}k" else "$amount",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        amounts.drop(3).forEach { amount ->
                            FilterChip(
                                selected = selectedAmount == amount,
                                onClick = { selectedAmount = amount },
                                label = {
                                    Text(
                                        if (amount >= 1000) "${amount / 1000}k" else "$amount",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zap comment
                OutlinedTextField(
                    value = zapComment,
                    onValueChange = { zapComment = it },
                    placeholder = { Text("Add a comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                        cursorColor = MaterialTheme.colorScheme.tertiary,
                    ),
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Opens your Lightning wallet to complete the zap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            val lud16 = creatorProfile?.lud16
                            if (lud16.isNullOrBlank()) {
                                error = "Creator hasn't set up a Lightning address"
                                isLoading = false
                                return@launch
                            }

                            // Resolve LNURL
                            val payUrl = nip57Zap.resolveLud16(lud16)
                            if (payUrl == null) {
                                error = "Invalid Lightning address"
                                isLoading = false
                                return@launch
                            }

                            val params = withContext(Dispatchers.IO) {
                                nip57Zap.fetchLnurlPayParams(payUrl)
                            }
                            if (params == null) {
                                error = "Couldn't reach Lightning service"
                                isLoading = false
                                return@launch
                            }

                            val amountMsat = selectedAmount.toLong() * 1000

                            // For now, request invoice without zap request signing
                            // (full zap with Amber signing is a future enhancement)
                            val invoiceUrl = "${params.callback}?amount=$amountMsat"

                            val invoice = withContext(Dispatchers.IO) {
                                try {
                                    val request = okhttp3.Request.Builder().url(invoiceUrl).build()
                                    val client = okhttp3.OkHttpClient()
                                    val response = client.newCall(request).execute()
                                    if (response.isSuccessful) {
                                        val json = kotlinx.serialization.json.Json.parseToJsonElement(
                                            response.body?.string() ?: ""
                                        )
                                        (json as? kotlinx.serialization.json.JsonObject)
                                            ?.get("pr")
                                            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                    } else null
                                } catch (_: Exception) { null }
                            }

                            if (invoice != null) {
                                // Open Lightning wallet
                                val lightningIntent = Intent(Intent.ACTION_VIEW, Uri.parse("lightning:$invoice"))
                                try {
                                    context.startActivity(lightningIntent)
                                    onDismiss()
                                } catch (_: Exception) {
                                    // No Lightning wallet — try copying to clipboard
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("invoice", invoice))
                                    Toast.makeText(context, "Invoice copied! Paste in your Lightning wallet", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            } else {
                                error = "Couldn't get invoice from Lightning service"
                            }

                            isLoading = false
                        } catch (e: Exception) {
                            error = e.message ?: "Zap failed"
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("⚡ Zap $selectedAmount sats")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
