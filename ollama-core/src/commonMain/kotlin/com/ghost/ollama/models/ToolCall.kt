package com.ghost.ollama.models

import kotlinx.serialization.Serializable


@Serializable
data class ToolCall(
    val function: ToolFunction
)