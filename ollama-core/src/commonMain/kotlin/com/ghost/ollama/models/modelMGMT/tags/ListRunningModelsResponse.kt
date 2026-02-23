package com.ghost.ollama.models.modelMGMT.tags

import kotlinx.serialization.Serializable

@Serializable
data class ListRunningModelsResponse(
    val models: List<RunningModelInfo>
)