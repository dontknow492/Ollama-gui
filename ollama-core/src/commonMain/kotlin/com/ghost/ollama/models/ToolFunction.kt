package com.ghost.ollama.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


@Serializable
data class ToolFunction(
    val name: String,
    val description: String? = null,
    val arguments: JsonObject? = null
)