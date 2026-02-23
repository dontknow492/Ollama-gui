package com.ghost.ollama

class JVMPlatform : Platform {
    override val name: String = "JVM"
}

actual fun getPlatform(): Platform = JVMPlatform()
