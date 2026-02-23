package com.ghost.ollama.gui.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.ghost.ollama.OllamaClient
import com.ghost.ollama.createHttpClient
import com.ghost.ollama.gui.ui.`interface`.chat.ChatScreen
import com.ghost.ollama.gui.ui.theme.AppTheme
import com.ghost.ollama.models.common.ThinkBoolean

//val dsat = Message

@Composable
fun App() {
//    Message
    val client = remember { OllamaClient(httpClient = createHttpClient()) }
    var versionText by remember { mutableStateOf("Loading...") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AppTheme {
        LaunchedEffect(Unit) {
            try {
                val version = client.generate(
                    "qwen3:4b",
                    think = ThinkBoolean(false),
                    prompt = "Hello",
//                    messages = listOf(ChatMessage(role = ChatMessage.Role.USER, content = "Hello"))
                )
                versionText = version.response ?: "faaaa"
//                version.onEach { delay(100) }.collect {
//                    versionText += it.thinking
//                }
//                versionText = version.toString()
            } catch (e: Exception) {
                errorText = "Error: ${e.message}"
                versionText = "Failed to load version"
            }
        }
        Surface {
            SelectionContainer {
                if (errorText != null) {
                    Text(errorText!!)
                } else {
                    Text(versionText)
                }
            }
            ChatScreen(
//                viewModel = koinViewModel()
            )
//            EmptySessionScreen(
//                userName = "Ollama",
//                inputBarState = InputBarState("$versionText", "Send a message"),
//                onInputChanged = {},
//                onSuggestionClick = {}
//            )
        }
    }
}
