package com.ghost.ollama.gui.viewmodel.download

import com.ghost.ollama.enum.PullStatus
import com.ghost.ollama.gui.ModelEntity

data class ActiveDownload(
    val tag: String,
    val modelEntity: ModelEntity, // Full SQLDelight entity for UI display
    val status: PullStatus,
    val progress: Float = 0f,
    val message: String? = null
)