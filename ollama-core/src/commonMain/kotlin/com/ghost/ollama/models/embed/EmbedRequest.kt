package com.ghost.ollama.models.embed

import kotlinx.serialization.Serializable

@Serializable
data class EmbedRequest(
    val model: String,
    val input: String
)