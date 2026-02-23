package com.ghost.ollama.enum

import kotlinx.serialization.Serializable

@Serializable
enum class PullStatus {
    pulling,
    done,
    error,
    queued
}

