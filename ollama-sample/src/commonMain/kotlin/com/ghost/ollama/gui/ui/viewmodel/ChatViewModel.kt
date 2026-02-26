package com.ghost.ollama.gui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import com.ghost.ollama.exception.OllamaNetworkException
import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.repository.OllamaRepository
import com.ghost.ollama.gui.ui.components.TuneOptions
import com.ghost.ollama.gui.utils.mapToUiChatMessages
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
    val session: SessionView? = null,
    val isGenerating: Boolean = false // Overall chat lock state
)

sealed class ChatSideEffect {
    data class CopyToClipboard(val text: String) : ChatSideEffect()
    data class ShowToast(val message: String) : ChatSideEffect()
}


sealed interface ChatEvent {
    data class SessionSelected(val sessionId: String) : ChatEvent
    data class SendMessage(val content: String, val model: String = "qwen3:4b", val images: List<String>? = null) :
        ChatEvent

    object StopGeneration : ChatEvent
    data class DeleteMessage(val messageId: String) : ChatEvent
    data class CopyMessage(val messageId: String) : ChatEvent
    object ClearChat : ChatEvent
    object RetryLastMessage : ChatEvent
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
//    private val sessionId = MutableStateFlow(generateSessionId())
    // Which session is active
    private val currentSessionId = MutableStateFlow<String?>(null)

    // Reactive session
    @OptIn(ExperimentalCoroutinesApi::class)
    private val session: StateFlow<SessionView?> =
        currentSessionId
            .flatMapLatest { id ->
                id?.let {
                    ollamaRepository.getSessionById(it)
                } ?: ollamaRepository.getOrCreateActiveSession()
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    // Generation state
    private var currentStreamingJob: Job? = null
    private val _isGenerating = MutableStateFlow(false)

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    val state: StateFlow<ChatUiState> =
        combine(
            session,
            _isGenerating
        ) { session, isGenerating ->
            ChatUiState(
                session = session,
                isGenerating = isGenerating
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ChatUiState()
            )

    private val _effect = MutableSharedFlow<ChatSideEffect>()
    val effect: SharedFlow<ChatSideEffect> = _effect.asSharedFlow()


    // -------------------------------------------------------------------------
    // Messages flow (paged)
    // -------------------------------------------------------------------------
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: Flow<PagingData<UiChatMessage>> =
        session
            .filterNotNull()
            .flatMapLatest { current ->
                Napier.d(
                    tag = "ChatViewModel",
                    message = "Switching to session: ${current.id}-${current.title}"
                )

                ollamaRepository
                    .getMessagesPaged(current.id)
                    .mapToUiChatMessages(
                        sessionId = current.id,
                        isStreaming = _isGenerating.value
                    )
            }


    init {
        Napier.d(tag = "ChatViewModel", message = "ViewModel initialized with sessionId: ${currentSessionId}")

        viewModelScope.launch {
            val initial = ollamaRepository.getOrCreateActiveSession().first()
            currentSessionId.value = initial.id

        }
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.SessionSelected -> setCurrentSession(event.sessionId)
            is ChatEvent.SendMessage -> sendMessage(event.content, event.model, event.images)
            ChatEvent.StopGeneration -> stopGeneration()
            is ChatEvent.DeleteMessage -> deleteMessage(event.messageId)
            is ChatEvent.CopyMessage -> copyMessage(event.messageId)
            ChatEvent.ClearChat -> clearChat()
            ChatEvent.RetryLastMessage -> retryLastMessage()
        }
    }

    /**
     * Switch to a different chat session.
     */
    private fun setCurrentSession(newSessionId: String) {
        Napier.i(tag = "ChatViewModel", message = "Setting current Session ID: $newSessionId")
        viewModelScope.launch {
            cancelCurrentGeneration()
            val exists = ollamaRepository.sessionExists(sessionId = newSessionId)
            if (!exists) {
                Napier.w(
                    tag = "ChatViewModel",
                    message = "Session not found: $newSessionId, creating new one"
                )
            } else {
                Napier.d(
                    tag = "ChatViewModel",
                    message = "Switching to existing session: $newSessionId"
                )
                currentSessionId.value = newSessionId
            }
        }
    }

    /**
     * Send a new user message and start streaming the assistant's response.
     */
    private fun sendMessage(content: String, model: String = "qwen3:4b", images: List<String>? = null) {
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
            val activeSession = session.value ?: return@launch

            _isGenerating.value = true

            try {
                ollamaRepository.sendChatMessageStreaming(
                    sessionId = activeSession.id,
                    modelName = model,
                    content = content,
                    role = MessageRole.USER
                ).collect { response ->
                    handleStreamResponse(response)
                }
            } catch (e: OllamaNetworkException) {
                Napier.e(tag = "ChatViewModel", message = "Network error during generation", throwable = e)
                handleError(Exception("Network error: ${e.message}"))
            } catch (e: CancellationException) {
                handleCancellation()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isGenerating.value = false
                currentStreamingJob = null
            }
        }
    }

    /**
     * Stop the ongoing generation.
     */
    private fun stopGeneration() {
        Napier.d(tag = "ChatViewModel", message = "stopGeneration called")
        cancelCurrentGeneration()
    }

    /**
     * Delete a specific message.
     */
    private fun deleteMessage(messageId: String) {
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
    private fun copyMessage(messageId: String) {
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
     * Clear the current chat (starts a new session).
     */
    private fun clearChat() {
        if (currentSessionId.value != null) {
            Napier.d(tag = "ChatViewModel", message = "clearChat: clearing session ${currentSessionId.value}")
            viewModelScope.launch {
                cancelCurrentGeneration()
                if (currentSessionId.value == null) return@launch
                ollamaRepository.clearSession(sessionId = currentSessionId.value!!)
                _effect.emit(ChatSideEffect.ShowToast("Chat cleared"))
            }
        } else {
            Napier.w(tag = "ChatViewModel", message = "clearChat: no active session to clear")
        }

    }

    /**
     * Retry the last user message.
     */
    private fun retryLastMessage() {
        viewModelScope.launch(ioDispatcher) {
            val activeSession = session.value ?: return@launch
            val lastUser = ollamaRepository.getLastUserMessage(activeSession.id)
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
        Napier.i(
            tag = "ChatViewModel",
            message = "Stream cancelled by user"
        )

        withContext(Dispatchers.Main) {
            _effect.emit(
                ChatSideEffect.ShowToast(
                    "Generation stopped. Partial response saved."
                )
            )
        }

        markLastAssistantAsErrored("Generation stopped by user")
    }

    private suspend fun handleError(e: Exception) {
        Napier.e(
            tag = "ChatViewModel",
            message = "Stream error",
            throwable = e
        )

        val userMessage = mapToUserFriendlyMessage(e)

        withContext(Dispatchers.Main) {
            _effect.emit(ChatSideEffect.ShowToast(userMessage))
        }

        markLastAssistantAsErrored(userMessage)
    }

    private fun mapToUserFriendlyMessage(e: Exception): String {
        return when (e) {
            is OllamaNetworkException -> {
                if (e.message?.contains("Connection refused") == true) {
                    "Unable to connect to Ollama.\nMake sure the Ollama server is running on localhost:11434."
                } else {
                    "Network error while contacting Ollama."
                }
            }

            is CancellationException -> {
                "Request was cancelled."
            }

            else -> {
                "Something went wrong while generating the response."
            }
        }
    }

    private suspend fun markLastAssistantAsErrored(errorText: String) {
        withContext(ioDispatcher) {
            val activeSession = session.value ?: return@withContext

            Napier.d(
                tag = "ChatViewModel",
                message = "Marking last assistant message as error in session=${activeSession.id} : $errorText"
            )

            val lastMsg = ollamaRepository.getLastUserMessage(activeSession.id)

            if (lastMsg != null &&
                lastMsg.role == MessageRole.ASSISTANT &&
                lastMsg.state != MessageState.Done
            ) {
                ollamaRepository.updateMessageError(
                    messageId = lastMsg.id,
                    isError = true,
                    error = errorText
                )
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