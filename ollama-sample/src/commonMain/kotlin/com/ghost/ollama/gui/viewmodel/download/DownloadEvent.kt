package com.ghost.ollama.gui.viewmodel.download

import com.ghost.ollama.gui.ModelEntity

// --- EVENTS ---
sealed interface DownloadEvent {
    // Library interactions
    data class SearchQueryChanged(val query: String) : DownloadEvent
    data class CapabilityFilterChanged(val capability: String?) : DownloadEvent
    data class ModelSelected(val model: ModelEntity?) : DownloadEvent

    // Download interactions
    data class StartDownload(val tag: String, val modelEntity: ModelEntity) : DownloadEvent
    data class PauseDownload(val tag: String) : DownloadEvent
    data class ResumeDownload(val tag: String) : DownloadEvent
    data class CancelDownload(val tag: String) : DownloadEvent
    data class DismissDownload(val tag: String) : DownloadEvent
}