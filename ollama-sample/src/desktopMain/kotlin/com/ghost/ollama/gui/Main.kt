package com.ghost.ollama.gui

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ghost.ollama.OllamaClient
import com.ghost.ollama.createHttpClient
import com.ghost.ollama.enum.ResponseFormat
import com.ghost.ollama.exception.OllamaException
import com.ghost.ollama.gui.di.appModule
import com.ghost.ollama.gui.ui.App
import com.ghost.ollama.models.chat.ChatOptions
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin

fun main() = application {

    startKoin {
        modules(appModule)
    }
    Napier.base(DebugAntilog())

    Window(
        title = "Ollama Sample",
        onCloseRequest = ::exitApplication,
        icon = painterResource("ollama.png")
    ) {
        App()
    }
}

