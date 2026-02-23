package com.ghost.ollama.exceptions

import kotlinx.serialization.Serializable

@Serializable
data class OllamaErrorResponse(
    val error: String? = null
)