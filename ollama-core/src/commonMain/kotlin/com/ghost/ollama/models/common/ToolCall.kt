package com.ghost.ollama.models.common

import kotlinx.serialization.Serializable

@Serializable
data class ToolCall(
    val function: ToolFunction
)