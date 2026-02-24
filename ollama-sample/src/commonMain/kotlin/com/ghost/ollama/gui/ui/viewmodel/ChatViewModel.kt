package com.ghost.ollama.gui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import com.ghost.ollama.gui.repository.OllamaRepository
import com.ghost.ollama.gui.utils.mapToUiChatMessages
import com.ghost.ollama.models.chat.ChatOptions
import com.ghost.ollama.models.chat.ChatResponse
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import com.ghost.ollama.models.chat.ChatMessage.Role as MessageRole

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

data class UiChatMessage(
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
    val messages: PagingData<UiChatMessage> = PagingData.empty(), // Using PagingData for efficient loading of large chat histories
    val title: String = "New Chat", // Optional session title
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    // -------------------------------------------------------------------------
    // Session and options
    // -------------------------------------------------------------------------
    private val sessionId = MutableStateFlow(generateSessionId())
    private val chatOptions = MutableStateFlow(ChatOptions(temperature = 0.7f))

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<ChatSideEffect>()
    val effect: SharedFlow<ChatSideEffect> = _effect.asSharedFlow()

    // Generation state
    private var currentStreamingJob: Job? = null
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // -------------------------------------------------------------------------
    // Messages flow (paged)
    // -------------------------------------------------------------------------
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: Flow<PagingData<UiChatMessage>> = sessionId
        .flatMapLatest { currentId ->
            Napier.d(tag = "ChatViewModel", message = "Switching to session: $currentId")
            ollamaRepository.getMessagesPaged(currentId).mapToUiChatMessages(
                sessionId = currentId,
                isStreaming = _isGenerating.value
            )

        }

    init {
        Napier.d(tag = "ChatViewModel", message = "ViewModel initialized with sessionId: ${sessionId.value}")
        observeSessionTitle()
        collectMessages() // optional: if you want to update state with paging data
    }

    /**
     * Collects messages and updates the state's messages field.
     * This is optional if you want to keep the messages inside ChatUiState.
     * Alternatively, the UI can collect `messages` directly.
     */
    private fun collectMessages() {
        viewModelScope.launch {
            messages.collectLatest { pagingData ->
                _state.update { it.copy(messages = pagingData) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSessionTitle() {
        viewModelScope.launch {
            sessionId
                .flatMapLatest { currentId ->
                    ollamaRepository.getSessionById(currentId)
                }
                .collectLatest { session ->
                    _state.update { it.copy(title = session?.title ?: "New Chat") }
                    Napier.d(tag = "ChatViewModel", message = "Session title updated: ${session?.title}")
                }
        }
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Switch to a different chat session.
     */
    fun setCurrentSession(newSessionId: String) {
        Napier.d(tag = "ChatViewModel", message = "Switching session from ${sessionId.value} to $newSessionId")
        sessionId.value = newSessionId
    }

    /**
     * Send a new user message and start streaming the assistant's response.
     */
    fun sendMessage(content: String, model: String = "qwen3:4b", images: List<String>? = null) {
        if (_isGenerating.value) {
            Napier.w(tag = "ChatViewModel", message = "sendMessage ignored: already generating")
            return
        }

//        val model = chatOptions.value.model.ifEmpty { "qwen3:4b" } // fallback
        Napier.d(
            tag = "ChatViewModel",
            message = "sendMessage: content='$content', model=$model, images=${images?.size}"
        )

        cancelCurrentGeneration() // ensure no stale job

        currentStreamingJob = viewModelScope.launch(ioDispatcher) {
            _isGenerating.value = true
            _state.update { it.copy(isGenerating = true) }

            try {
                ollamaRepository.sendChatMessageStreaming(
                    sessionId = sessionId.value,
                    modelName = model,
                    content = content,
                    role = MessageRole.USER
                ).collect { response ->
                    handleStreamResponse(response)
                }
            } catch (e: CancellationException) {
                handleCancellation()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isGenerating.value = false
                _state.update { it.copy(isGenerating = false) }
                currentStreamingJob = null
                Napier.d(tag = "ChatViewModel", message = "Streaming finished (finally)")
            }
        }
    }

    /**
     * Stop the ongoing generation.
     */
    fun stopGeneration() {
        Napier.d(tag = "ChatViewModel", message = "stopGeneration called")
        cancelCurrentGeneration()
    }

    /**
     * Delete a specific message.
     */
    fun deleteMessage(messageId: String) {
        Napier.d(tag = "ChatViewModel", message = "deleteMessage: $messageId")
        viewModelScope.launch(ioDispatcher) {
            try {
                ollamaRepository.deleteChatMessage(messageId)
            } catch (e: Exception) {
                Napier.e(tag = "ChatViewModel", message = "Failed to delete message", throwable = e)
            }
        }
    }

    /**
     * Copy message content to clipboard.
     */
    fun copyMessage(messageId: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val message = ollamaRepository.getMessageById(messageId)
                if (message != null) {
                    val content = message.content ?: ""
                    _effect.emit(ChatSideEffect.CopyToClipboard(content))
                    Napier.d(tag = "ChatViewModel", message = "copyMessage: emitted copy effect for message $messageId")
                } else {
                    Napier.w(tag = "ChatViewModel", message = "copyMessage: message not found $messageId")
                }
            } catch (e: Exception) {
                Napier.e(tag = "ChatViewModel", message = "copyMessage failed", throwable = e)
            }
        }
    }

    /**
     * Update chat options (temperature, model, etc.)
     */
    fun updateOptions(newOptions: ChatOptions) {
        Napier.d(tag = "ChatViewModel", message = "updateOptions: $newOptions")
        chatOptions.value = newOptions
        _state.update { it.copy(options = newOptions) }
    }

    /**
     * Clear the current chat (starts a new session).
     */
    fun clearChat() {
        Napier.d(tag = "ChatViewModel", message = "clearChat")
        viewModelScope.launch {
            cancelCurrentGeneration()
            sessionId.value = generateSessionId()
            _state.update {
                it.copy(
                    messages = PagingData.empty(), // reset paging data
                    isGenerating = false
                )
            }
            _effect.emit(ChatSideEffect.ShowToast("Chat cleared"))
        }
    }

    /**
     * Retry the last user message.
     */
    fun retryLastMessage() {
        viewModelScope.launch(ioDispatcher) {
            val lastUser = ollamaRepository.getLastUserMessage(sessionId.value)
            if (lastUser != null) {
                Napier.d(tag = "ChatViewModel", message = "retryLastMessage: resending '${lastUser.content}'")
                withContext(Dispatchers.Main) {
                    sendMessage(
                        content = lastUser.content ?: "",
                        images = lastUser.images
                    )
                }
            } else {
                Napier.w(tag = "ChatViewModel", message = "retryLastMessage: no user message found")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun cancelCurrentGeneration() {
        currentStreamingJob?.cancel(CancellationException("Generation stopped by user"))
        currentStreamingJob = null
        _isGenerating.value = false
        _state.update { it.copy(isGenerating = false) }
    }

    private suspend fun handleStreamResponse(response: ChatResponse) {
        Napier.v(
            tag = "ChatViewModel",
            message = "Stream chunk: done=${response.done}, content length=${response.message.content.length}"
        )
        if (response.done) {
            withContext(Dispatchers.Main) {
                _effect.emit(ChatSideEffect.ShowToast("Message completed"))
            }
            Napier.d(tag = "ChatViewModel", message = "Stream completed. Reason: ${response.doneReason}")
        }
    }

    private suspend fun handleCancellation() {
        withContext(Dispatchers.Main) {
            _effect.emit(ChatSideEffect.ShowToast("Generation stopped"))
        }
        Napier.i(tag = "ChatViewModel", message = "Stream cancelled by user")
        // Optionally mark the last assistant message as errored
        markLastAssistantAsErrored("Stopped by user")
    }

    private suspend fun handleError(e: Exception) {
        Napier.e(tag = "ChatViewModel", message = "Stream error", throwable = e)
        withContext(Dispatchers.Main) {
            _effect.emit(ChatSideEffect.ShowToast("Error: ${e.message}"))
        }
        markLastAssistantAsErrored(e.message ?: "Unknown error")
    }

    private suspend fun markLastAssistantAsErrored(errorText: String) {
        withContext(ioDispatcher) {
            val lastMsg = ollamaRepository.getLastUserMessage(sessionId.value)
            if (lastMsg != null && lastMsg.role == MessageRole.ASSISTANT && lastMsg.state != MessageState.Done) {
                ollamaRepository.updateMessageError(messageId = lastMsg.id, isError = true, error = errorText)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Napier.d(tag = "ChatViewModel", message = "onCleared, cancelling any remaining job")
        cancelCurrentGeneration()
    }

    private fun generateSessionId(): String {
        return System.currentTimeMillis().toString(16)
    }
}