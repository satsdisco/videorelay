package com.videorelay.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videorelay.app.data.db.DownloadEntity
import com.videorelay.app.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val downloads: StateFlow<List<DownloadEntity>> = downloadRepository
        .observeDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            // Delete local file if exists
            if (download.localPath.isNotBlank()) {
                try {
                    java.io.File(download.localPath).delete()
                } catch (_: Exception) {}
            }
            downloadRepository.delete(download)
        }
    }
}
