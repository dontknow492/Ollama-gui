package com.ghost.ollama.gui.utils

import com.ghost.ollama.gui.Message
import com.ghost.ollama.models.chat.ChatResponse
import com.ghost.ollama.models.generate.GenerateResponse
import kotlinx.serialization.json.Json

/**
 * Maps a ChatResponse from the Ollama API to the SQLDelight Message entity.
 */
fun ChatResponse.toEntity(
    id: String,
    sessionId: String,
    createdAtMillis: Long,
    errorMessage: String?,
    json: Json = Json
): Message {
    val errored = errorMessage != null
    return Message(
        id = id,
        session_id = sessionId,
        model_name = this.model,
        role = this.message.role.name.lowercase(), // e.g., "assistant"
        content = this.message.content,
        thinking = this.message.thinking,
        images = this.message.images,
        // Manually encode objects to String since no ColumnAdapter is attached to these in SQLDelight
        tool_calls = this.message.toolCalls?.let { json.encodeToString(it) },
        logprobs = this.logprobs?.let { json.encodeToString(it) },
        created_at = createdAtMillis,
        is_done = this.done,
        done_reason = this.doneReason,
        total_duration = this.totalDuration,
        load_duration = this.loadDuration,
        prompt_eval_count = this.promptEvalCount?.toLong(),
        prompt_eval_duration = this.promptEvalDuration,
        eval_count = this.evalCount?.toLong(),
        eval_duration = this.evalDuration,
        errored = errored,
        error_message = errorMessage
    )
}

/**
 * Maps a GenerateResponse from the Ollama API to the SQLDelight Message entity.
 */
fun GenerateResponse.toEntity(
    id: String,
    sessionId: String,
    createdAtMillis: Long,
    errorMessage: String?,
    json: Json = Json
): Message {
    val errored = errorMessage != null
    return Message(
        id = id,
        session_id = sessionId,
        model_name = this.model,
        role = "assistant", // Generate completions are always from the assistant
        content = this.response,
        thinking = this.thinking,
        images = null, // Generate endpoint does not process/return output images
        tool_calls = null, // Generate endpoint does not support tool calls
        logprobs = this.logprobs?.let { json.encodeToString(it) },
        created_at = createdAtMillis,
        is_done = this.done,
        done_reason = this.doneReason,
        total_duration = this.totalDuration,
        load_duration = this.loadDuration,
        prompt_eval_count = this.promptEvalCount?.toLong(),
        prompt_eval_duration = this.promptEvalDuration,
        eval_count = this.evalCount?.toLong(),
        eval_duration = this.evalDuration,
        errored = errored,
        error_message = errorMessage
    )
}