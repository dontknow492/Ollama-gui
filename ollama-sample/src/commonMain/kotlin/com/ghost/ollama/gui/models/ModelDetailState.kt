package com.ghost.ollama.gui.models

import com.ghost.ollama.models.modelMGMT.ShowModelResponse

sealed interface ModelDetailState {
    data object Loading : ModelDetailState
    data class Success(val data: ShowModelResponse) : ModelDetailState
    data class Error(val message: String) : ModelDetailState
    object Idle : ModelDetailState
}