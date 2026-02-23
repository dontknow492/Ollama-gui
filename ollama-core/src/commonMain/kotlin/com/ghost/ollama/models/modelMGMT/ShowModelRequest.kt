package com.ghost.ollama.models.modelMGMT

import kotlinx.serialization.Serializable

@Serializable
data class ShowModelRequest(
    val name: String,
    val verbose: Boolean
)