package com.ghost.ollama.gui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghost.ollama.gui.repository.OllamaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import com.ghost.ollama.models.chat.ChatMessage.Role as MessageRole


// --- DATA MODELS ---

@Serializable
enum class ResponseFormat {
    @SerialName("json")
    JSON,
    @SerialName("text")
    TEXT
}

@Serializable
data class ChatOptions(
    val seed: Int? = null,
    val temperature: Float? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    @SerialName("min_p")
    val minP: Float? = null,
    val stop: String? = null,
    @SerialName("num_ctx")
    val numCtx: Int? = null,
    @SerialName("num_predict")
    val numPredict: Int? = null,
    val format: ResponseFormat? = null
)

/**
 * Represents the granular sub-state of an individual message,
 * particularly useful for tracking the AI's generation process.
 */
sealed class MessageState {
    object Idle : MessageState() // Default state for user/system messages
    object Loading : MessageState() // Request sent, waiting for first byte/token
    data class Generating(val isThinking: Boolean = false) : MessageState() // Actively streaming tokens
    object Done : MessageState() // Generation completed successfully
    data class Errored(val error: String) : MessageState() // Failed to generate/send
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val modelName: String = "",
    val role: MessageRole,
    val content: String? = null,
    val state: MessageState = MessageState.Idle,
    val thinking: String? = null,
    val images: List<String>? = null,
    val toolCalls: String? = null,
    val logprobs: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDone: Boolean? = null,
    val doneReason: String? = null,
    val totalDuration: Long? = null,
    val loadDuration: Long? = null,
    val promptEvalCount: Long? = null,
    val promptEvalDuration: Long? = null,
    val evalCount: Long? = null,
    val evalDuration: Long? = null
)
// --- STATE & SIDE EFFECTS ---

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val options: ChatOptions = ChatOptions(temperature = 0.7f), // Default options
    val isGenerating: Boolean = false // Overall chat lock state
)

sealed class ChatSideEffect {
    data class CopyToClipboard(val text: String) : ChatSideEffect()
    data class ShowToast(val message: String) : ChatSideEffect()
}

// --- VIEW MODEL ---

/**
 * Note: If using Android, this would inherit from androidx.lifecycle.ViewModel.
 * Here it is presented as a plain class for broader Kotlin compatibility,
 * but you would scope the coroutines to `viewModelScope`.
 */
class ChatViewModel(
    private val ollamaRepository: OllamaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ChatSideEffect>()
    val effect: SharedFlow<ChatSideEffect> = _effect.asSharedFlow()

    /**
     * Sends a new user message and triggers the AI response simulation.
     */
    fun sendMessage(text: String) {
        if (text.isBlank() || _state.value.isGenerating) return

        val userMessage = ChatMessage(
            sessionId = "default-session",
            role = MessageRole.USER,
            content = text,
            state = MessageState.Done,
            isDone = true
        )

        val assistantMessageId = UUID.randomUUID().toString()
        val initialAssistantMessage = ChatMessage(
            id = assistantMessageId,
            sessionId = "default-session",
            role = MessageRole.ASSISTANT,
            content = "",
            state = MessageState.Loading,
            isDone = false
        )

        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage + initialAssistantMessage,
                isGenerating = true
            )
        }

        // Trigger the AI generation process
        generateAiResponse(assistantMessageId)
    }

    /**
     * Deletes a specific message by its ID.
     */
    fun deleteMessage(messageId: String) {
        _state.update { currentState ->
            currentState.copy(
                messages = currentState.messages.filterNot { it.id == messageId }
            )
        }
    }

    /**
     * Emits a side effect to the UI to copy the message content to the device clipboard.
     */
    fun copyMessage(messageId: String) {
        val messageToCopy = _state.value.messages.find { it.id == messageId }
        messageToCopy?.content?.let { content ->
            viewModelScope.launch {
                _effect.emit(ChatSideEffect.CopyToClipboard(content))
            }
        }
    }

    /**
     * Updates the chat options (temperature, top_p, etc.).
     */
    fun updateOptions(newOptions: ChatOptions) {
        _state.update { it.copy(options = newOptions) }
    }

    /**
     * Clears the entire chat history.
     */
    fun clearChat() {
        _state.update { it.copy(messages = emptyList(), isGenerating = false) }
    }

    /**
     * Internal helper to update a specific message's content and state.
     */
    private fun updateMessage(messageId: String, content: String, state: MessageState, isDone: Boolean? = null) {
        _state.update { currentState ->
            val updatedMessages = currentState.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        content = content,
                        state = state,
                        isDone = isDone ?: msg.isDone
                    )
                } else {
                    msg
                }
            }
            currentState.copy(messages = updatedMessages)
        }
    }

    /**
     * Simulates a network call and streaming response.
     * In a real app, this would connect to an LLM repository/API.
     */
    private fun generateAiResponse(messageId: String) {
        viewModelScope.launch {
            try {
                // 1. Simulate Network Delay (Loading State)
                delay(800)

                // 2. Transition to Generating (Thinking)
                updateMessage(messageId, "", MessageState.Generating(isThinking = true))
                delay(1000) // Simulating "Thinking..." time

                // 3. Start streaming tokens (Generating)
                val simulatedTokens = listOf(
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                    "Here ",
                    "is ",
                    "your ",
                    "response ",
                    "based ",
                    "on ",
                    "the ",
                    "options.",
                )
                var currentContent = ""

                for (token in simulatedTokens) {
                    currentContent += token
                    updateMessage(messageId, currentContent, MessageState.Generating(isThinking = false))
                    delay(10) // Simulate streaming delay between tokens
                }

                // 4. Mark as Done
                updateMessage(messageId, currentContent, MessageState.Done, isDone = true)

            } catch (e: Exception) {
                // 5. Handle Errors
                updateMessage(
                    messageId,
                    "An error occurred.",
                    MessageState.Errored(e.message ?: "Unknown Error"),
                    isDone = true
                )
                _effect.emit(ChatSideEffect.ShowToast("Failed to generate response"))
            } finally {
                // Unlock the chat input
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }
}