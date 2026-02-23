package com.ghost.ollama

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
