package com.ghost.ollama.gui

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
import org.koin.core.context.startKoin

fun main() = application {

    startKoin {
        modules(appModule)
    }
    Napier.base(DebugAntilog())

    Window(
        title = "Ollama Sample",
        onCloseRequest = ::exitApplication
    ) {
        App()
    }
}


fun mainClient() = runBlocking {
    try {
        val client = OllamaClient(httpClient = createHttpClient())
        client.ollamaVersion()
        client.listModels()
        client.listRunningModels()

        client.showModel("qwen3:4b", true)

        val chatOptions = ChatOptions(
            seed = 42,
            temperature = 0.7f,
            topK = 50,
            topP = 0.9f,
            minP = 0.1f,
            numCtx = 64000,
            numPredict = 15000,
            format = ResponseFormat.JSON // Assume ResponseFormat is an enum with different formats
        )

        val response = client.generate(
            model = "qwen3:4b",
            prompt = "hello",
            options = chatOptions
        )
        println(response)


//        val embed = client.embed("qwen3:4b", "Hello World")
    } catch (e: OllamaException) {
        println(e.message)
    }

//    print(embed)

}
