package com.ghost.ollama.models.modelMGMT.tags

import kotlinx.serialization.Serializable

@Serializable
data class ListModelsResponse(
    val models: List<ModelInfo>
)


