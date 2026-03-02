package com.ghost.ollama.enum

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PullStatus {
    @SerialName("pulling manifest")pulling,
    done,
    error,
    queued,
}

