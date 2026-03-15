package com.videorelay.app.ui.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.blossom.BlossomUploader
import com.videorelay.app.data.nostr.*
import com.videorelay.app.data.repository.RelayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class UploadUiState(
    val selectedUri: Uri? = null,
    val fileName: String = "",
    val fileSize: Long = 0,
    val title: String = "",
    val summary: String = "",
    val tags: String = "",
    val isShort: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: String = "",
    val isPublishing: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val videoUrl: String = "",
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blossomUploader: BlossomUploader,
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
    private val nsecSigner: NsecSigner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loggedIn = nsecSigner.isLoggedIn()
            _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
        }
    }

    fun selectVideo(uri: Uri) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        var name = "video"
        var size = 0L
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = it.getString(nameIdx)
                if (sizeIdx >= 0) size = it.getLong(sizeIdx)
            }
        }

        val maxSize = 500L * 1024 * 1024
        if (size > maxSize) {
            _uiState.value = _uiState.value.copy(
                error = "File too large (${size / 1024 / 1024}MB). Maximum is 500MB."
            )
            return
        }

        // Auto-detect if it should be marked as short based on filename
        val isShortGuess = name.contains("short", ignoreCase = true) ||
            name.contains("reel", ignoreCase = true) ||
            name.contains("clip", ignoreCase = true)

        _uiState.value = _uiState.value.copy(
            selectedUri = uri,
            fileName = name,
            fileSize = size,
            isShort = isShortGuess,
            error = null,
        )
    }

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(title = title) }
    fun updateSummary(summary: String) { _uiState.value = _uiState.value.copy(summary = summary) }
    fun updateTags(tags: String) { _uiState.value = _uiState.value.copy(tags = tags) }
    fun setIsShort(isShort: Boolean) { _uiState.value = _uiState.value.copy(isShort = isShort) }

    fun upload() {
        val state = _uiState.value
        val uri = state.selectedUri ?: return

        if (state.title.isBlank()) {
            _uiState.value = state.copy(error = "Title is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isUploading = true, error = null, uploadProgress = "Preparing upload...")

            try {
                // Copy URI to temp file
                val tempFile = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file
                }

                _uiState.value = _uiState.value.copy(uploadProgress = "Signing upload authorization...")

                // Build and sign a Blossom auth event (kind 24242) using local nsec signer
                val authEvent: NostrEvent? = if (nsecSigner.isLoggedIn()) {
                    try {
                        val fileHash = withContext(Dispatchers.IO) { blossomUploader.sha256File(tempFile) }
                        Log.d("UploadVM", "File hash: $fileHash, pubkey: ${nsecSigner.publicKey}")
                        val unsignedAuth = blossomUploader.buildAuthEvent(fileHash)
                        val signed = nsecSigner.signEvent(unsignedAuth)
                        Log.d("UploadVM", "Auth event signed: ${signed != null}, id=${signed?.id}, sig_len=${signed?.sig?.length}")
                        signed
                    } catch (e: Exception) {
                        Log.e("UploadVM", "Auth signing failed: ${e.message}", e)
                        null
                    }
                } else {
                    Log.w("UploadVM", "Not logged in — uploading without auth")
                    null
                }

                Log.d("UploadVM", "Temp file: ${tempFile.absolutePath}, size=${tempFile.length()}, exists=${tempFile.exists()}")
                _uiState.value = _uiState.value.copy(uploadProgress = "Uploading to Blossom servers...")

                // Detect actual MIME type from URI
                val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                Log.d("UploadVM", "Uploading with mimeType=$mimeType")

                val (videoUrl, results) = blossomUploader.upload(
                    file = tempFile,
                    mimeType = mimeType,
                    signedAuthEvent = authEvent,
                )

                val successCount = results.count { it.success }
                Log.d("UploadVM", "Upload results: $successCount/${results.size} succeeded")
                results.forEach { Log.d("UploadVM", "  ${it.server}: success=${it.success}, error=${it.error}") }

                if (successCount == 0 || videoUrl == null) {
                    val errors = results.joinToString("\n") { r ->
                        "• ${r.server.removePrefix("https://").substringBefore("/")}:\n  ${r.error}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = "Upload failed on all servers:\n$errors",
                    )
                    tempFile.delete()
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    uploadProgress = "Uploaded to $successCount/${results.size} servers. Publishing to Nostr...",
                    isPublishing = true,
                    videoUrl = videoUrl,
                )

                // Build NIP-71 event tags
                val tagsList = state.tags.split(",", " ")
                    .map { it.trim().removePrefix("#").lowercase() }
                    .filter { it.isNotBlank() }

                val eventTags = mutableListOf(
                    listOf("url", videoUrl),
                    listOf("title", state.title),
                )
                if (state.summary.isNotBlank()) eventTags.add(listOf("summary", state.summary))
                tagsList.forEach { tag -> eventTags.add(listOf("t", tag)) }

                // If marked as short, add the "shorts" tag
                if (state.isShort && !tagsList.contains("short") && !tagsList.contains("shorts")) {
                    eventTags.add(listOf("t", "short"))
                }

                // Determine kind: 22 for shorts, 21 for regular video
                val videoKind = if (state.isShort) 22 else 21

                val unsignedEvent = UnsignedEvent(
                    kind = videoKind,
                    content = state.summary,
                    tags = eventTags,
                )

                // Sign with local nsec signer
                val signedEvent = nsecSigner.signEvent(unsignedEvent)

                if (signedEvent != null) {
                    val relays = relayRepository.getActiveRelays()
                    try {
                        relayPool.publish(relays, signedEvent)
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            isPublishing = false,
                            success = true,
                            uploadProgress = "Published! Your video is live on Nostr.",
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isUploading = false,
                            isPublishing = false,
                            success = true, // Uploaded to Blossom at least
                            uploadProgress = "Uploaded to Blossom. Relay publish failed: ${e.message}",
                        )
                    }
                } else {
                    // No signer (not logged in with nsec) — show URL for manual publish
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        isPublishing = false,
                        success = true,
                        uploadProgress = "Uploaded! Video URL: $videoUrl\n\nSign in with nsec to auto-publish.",
                    )
                }

                tempFile.delete()

            } catch (e: Exception) {
                Log.e("UploadVM", "Upload failed", e)
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    isPublishing = false,
                    error = e.message ?: "Upload failed",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = UploadUiState(isLoggedIn = _uiState.value.isLoggedIn)
    }
}
