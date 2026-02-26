package com.ghost.ollama.gui.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ghost.ollama.gui.repository.AppTheme
import com.ghost.ollama.gui.repository.SettingsRepository
import com.ghost.ollama.gui.ui.`interface`.chat.ChatScreen
import com.ghost.ollama.gui.ui.theme.OllamaTheme
import org.koin.compose.koinInject

//val dsat = Message

@Composable
fun App() {
//    Message
    val settingsRepository: SettingsRepository = koinInject()

    val themeMode by settingsRepository.getAppTheme()
        .collectAsState(initial = AppTheme.SYSTEM)
//    val appTheme: AppTheme = settingRepositor

    OllamaTheme(themeMode = themeMode) {
//        LaunchedEffect(Unit) {
//            try {
//                val version = client.generate(
//                    "qwen3:4b",
//                    think = ThinkBoolean(false),
//                    prompt = "Hello",
////                    messages = listOf(ChatMessage(role = ChatMessage.Role.USER, content = "Hello"))
//                )
//                versionText = version.response ?: "faaaa"
////                version.onEach { delay(100) }.collect {
////                    versionText += it.thinking
////                }
////                versionText = version.toString()
//            } catch (e: Exception) {
//                errorText = "Error: ${e.message}"
//                versionText = "Failed to load version"
//            }
//        }
        Surface {
//            SelectionContainer {
//                if (errorText != null) {
//                    Text(errorText!!)
//                } else {
//                    Text(versionText)
//                }
//            }
            ChatScreen()
        }
    }
}
