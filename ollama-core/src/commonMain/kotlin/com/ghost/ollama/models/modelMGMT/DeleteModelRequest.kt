package com.ghost.ollama.models.modelMGMT

import kotlinx.serialization.Serializable

@Serializable
data class DeleteModelRequest(
    val name: String
)