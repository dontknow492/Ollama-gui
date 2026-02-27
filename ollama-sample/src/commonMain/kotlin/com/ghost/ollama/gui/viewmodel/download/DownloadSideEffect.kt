package com.ghost.ollama.gui.viewmodel.download

// --- SIDE EFFECTS (One-off UI actions) ---
sealed interface DownloadSideEffect {
    data class ShowError(val message: String) : DownloadSideEffect
    data class ShowSuccess(val message: String) : DownloadSideEffect
    data class ShowSnackbar(val message: String) : DownloadSideEffect
}