package com.ghost.ollama.gui.viewmodel.download

import app.cash.paging.PagingData
import com.ghost.ollama.gui.ModelEntity
import com.ghost.ollama.gui.repository.ModelWithTags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// --- STATE ---
data class DownloadUiState(
    // Search and Filter parameters
    val searchQuery: String = "",
    val activeCapability: String? = null,

    // Currently selected model for details view
    val selectedModel: ModelWithTags? = null,

    // Active downloads map (Tag -> Progress/Status)
    val activeDownloads: Map<String, ActiveDownload> = emptyMap()
)