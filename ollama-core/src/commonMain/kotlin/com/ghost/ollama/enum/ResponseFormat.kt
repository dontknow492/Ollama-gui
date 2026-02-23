package com.ghost.ollama.enum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ResponseFormat {
    @SerialName("json")
    JSON
}