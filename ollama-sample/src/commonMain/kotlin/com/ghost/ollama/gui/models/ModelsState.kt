package com.ghost.ollama.gui.models

import com.ghost.ollama.models.modelMGMT.tags.ListModelsResponse

sealed interface ModelsState {
    data object Loading : ModelsState
    data class Success(val data: ListModelsResponse) : ModelsState
    data class Error(val message: String) : ModelsState
}

