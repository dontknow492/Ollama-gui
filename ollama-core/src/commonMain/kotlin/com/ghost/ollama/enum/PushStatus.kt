package com.ghost.ollama.enum

import kotlinx.serialization.Serializable

@Serializable
enum class PushStatus {
    pushing,
    done,
    error,
    queued,
    unknown
}