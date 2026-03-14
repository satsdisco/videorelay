package com.videorelay.app.ui.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
    val isUploading: Boolean = false,
    val uploadProgress: String = "",
    val isPublishing: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val videoUrl: String = "",
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blossomUploader: BlossomUploader,
    private val relayPool: RelayPool,
    private val relayRepository: RelayRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

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

        // Validate size
        val maxSize = 500L * 1024 * 1024
        if (size > maxSize) {
            _uiState.value = _uiState.value.copy(
                error = "File too large (${size / 1024 / 1024}MB). Maximum is 500MB."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            selectedUri = uri,
            fileName = name,
            fileSize = size,
            error = null,
        )
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateSummary(summary: String) {
        _uiState.value = _uiState.value.copy(summary = summary)
    }

    fun updateTags(tags: String) {
        _uiState.value = _uiState.value.copy(tags = tags)
    }

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
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    file
                }

                _uiState.value = _uiState.value.copy(uploadProgress = "Uploading to Blossom servers...")

                // Upload to Blossom (without auth for now — auth requires signer integration)
                val (videoUrl, results) = blossomUploader.upload(
                    file = tempFile,
                    mimeType = "video/mp4",
                )

                val successCount = results.count { it.success }
                _uiState.value = _uiState.value.copy(
                    uploadProgress = "Uploaded to $successCount/${results.size} servers. Publishing...",
                    isPublishing = true,
                    videoUrl = videoUrl,
                )

                // Build NIP-71 event
                val tagsList = state.tags.split(",", " ")
                    .map { it.trim().removePrefix("#") }
                    .filter { it.isNotBlank() }

                val eventTags = mutableListOf(
                    listOf("url", videoUrl),
                    listOf("title", state.title),
                )
                if (state.summary.isNotBlank()) {
                    eventTags.add(listOf("summary", state.summary))
                }
                tagsList.forEach { tag ->
                    eventTags.add(listOf("t", tag.lowercase()))
                }

                // TODO: Sign with Amber and publish
                // For now, just mark success with the URL
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    isPublishing = false,
                    success = true,
                    uploadProgress = "Video uploaded! Sign and publish with Amber to complete.",
                )

                // Cleanup
                tempFile.delete()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    isPublishing = false,
                    error = e.message ?: "Upload failed",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = UploadUiState()
    }
}
