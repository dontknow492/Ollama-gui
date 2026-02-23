package com.ghost.ollama.exception

import kotlinx.serialization.Serializable

@Serializable
data class OllamaErrorResponse(
    val error: String? = null
)