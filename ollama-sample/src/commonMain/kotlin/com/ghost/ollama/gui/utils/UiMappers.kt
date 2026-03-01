package com.ghost.ollama.gui.utils

import app.cash.paging.PagingData
import app.cash.paging.map
import com.ghost.ollama.gui.MessageView
import com.ghost.ollama.gui.viewmodel.MessageState
import com.ghost.ollama.gui.viewmodel.UiChatMessage
import com.ghost.ollama.models.chat.ChatResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.ghost.ollama.models.chat.ChatMessage.Role as MessageRole

/**
 * Extension function to map a ChatResponse to a ChatMessage
 * Handles both streaming (partial) and final responses
 */
@OptIn(ExperimentalUuidApi::class)
fun ChatResponse.toUiChatMessage(
    sessionId: String = "",
    isStreaming: Boolean = false,
    existingMessage: UiChatMessage? = null
): UiChatMessage {

    val isThinking = message.thinking != null && message.content.isNullOrEmpty()
    val isContentGenerating = !message.content.isNullOrEmpty() && !isThinking

    // Determine the message state based on response status
    val messageState = when {
        // Error cases take precedence
        doneReason == "error" -> MessageState.Errored("Generation failed")

        // Thinking state (for reasoning models)
        isThinking && isStreaming -> MessageState.Generating(isThinking = true)

        // Content generation
        isContentGenerating && isStreaming -> MessageState.Generating(isThinking = false)

        // Initial loading (waiting for first token)
        !done && !isStreaming -> MessageState.Loading

        // Successfully completed
        done && doneReason == "stop" -> MessageState.Done

        // Completed with other reasons (length, etc)
        done -> MessageState.Done

        // Fallback
        else -> MessageState.Idle
    }

    return UiChatMessage(
        id = existingMessage?.id ?: "msg_${Uuid.random()}",
        sessionId = sessionId,
        modelName = model,
        role = message.role,
        content = if (existingMessage != null && isStreaming) {
            // Append content for streaming responses
            existingMessage.content?.let { existingContent ->
                existingContent + (message.content ?: "")
            } ?: message.content
        } else {
            message.content
        },
        state = messageState,
        thinking = message.thinking, // If your ChatMessage has thinking field
        images = message.images,
        toolCalls = message.toolCalls?.toString(), // Convert to JSON string if needed
        logprobs = logprobs?.toString(),
        createdAt = existingMessage?.createdAt ?: System.currentTimeMillis(),
        isDone = done,
        doneReason = doneReason,
        totalDuration = totalDuration,
        loadDuration = loadDuration,
        promptEvalCount = promptEvalCount?.toLong(),
        promptEvalDuration = promptEvalDuration,
        evalCount = evalCount?.toLong(),
        evalDuration = evalDuration
    )
}


/**
 * Extension function to handle a stream of ChatResponses and build a complete message
 */
@OptIn(ExperimentalUuidApi::class)
fun Flow<ChatResponse>.collectToUiChatMessage(
    sessionId: String = ""
): Flow<UiChatMessage> = flow {
    var accumulatedMessage: UiChatMessage? = null

    collect { response ->
        accumulatedMessage = response.toUiChatMessage(
            sessionId = sessionId,
            isStreaming = true,
            existingMessage = accumulatedMessage
        )
        emit(accumulatedMessage!!)
    }

    // After stream completes, mark as done if not already
    accumulatedMessage?.let { finalMessage ->
        if (!finalMessage.isDone!!) {
            emit(
                finalMessage.copy(
                    state = MessageState.Done,
                    isDone = true,
                    doneReason = "stop"
                )
            )
        }
    }
}


fun MessageView.toUiChatMessage(
    sessionId: String,
): UiChatMessage {

    val isThinking = thinking != null && content.isNullOrEmpty()
    !content.isNullOrEmpty() && !isThinking

    val messageRole = when (role) {
        "assistant" -> MessageRole.ASSISTANT
        "user" -> MessageRole.USER
        "system" -> MessageRole.SYSTEM
        "tool" -> MessageRole.TOOL
        else -> MessageRole.ASSISTANT
    }
    val messageState = when {

        doneReason == "error" ->
            MessageState.Errored("Generation failed")

        !isDone && thinking != null && content.isNullOrEmpty() ->
            MessageState.Generating(isThinking = true)

        !isDone && !content.isNullOrEmpty() ->
            MessageState.Generating(isThinking = false)

        !isDone ->
            MessageState.Loading

        isDone ->
            MessageState.Done

        else ->
            MessageState.Idle
    }

    return UiChatMessage(
        id = id,
        sessionId = sessionId,
        modelName = modelName,
        role = messageRole,
        content = content,
        state = messageState,
        thinking = thinking, // If your ChatMessage has thinking field
        images = images,
        toolCalls = toolCalls, // Convert to JSON string if needed
        logprobs = logprobs,
        createdAt = createdAt,
        isDone = isDone,
        doneReason = doneReason,
        totalDuration = totalDuration,
        loadDuration = loadDuration,
        promptEvalCount = promptEvalCount,
        promptEvalDuration = promptEvalDuration,
        evalCount = evalCount,
        evalDuration = evalDuration
    )
}


/**
 * Extension function for Flow<PagingData<MessageView>> to easily convert to UiChatMessage
 */
fun Flow<PagingData<MessageView>>.mapToUiChatMessages(
    sessionId: String,
): Flow<PagingData<UiChatMessage>> {
    return this.map { pagingData ->
        pagingData.map { messageView ->
            messageView.toUiChatMessage(sessionId)
        }
    }
}