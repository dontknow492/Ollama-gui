package com.ghost.ollama.models.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ThinkOption


@Serializable
@SerialName("boolean")
data class ThinkBoolean(val value: Boolean) : ThinkOption

@Serializable
@SerialName("level")
data class ThinkLevel(val level: String) : ThinkOption // "high", "medium", "low"
