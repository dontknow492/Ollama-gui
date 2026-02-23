package com.ghost.ollama.models

import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val version: String
)