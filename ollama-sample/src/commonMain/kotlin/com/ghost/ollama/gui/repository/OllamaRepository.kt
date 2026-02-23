package com.ghost.ollama.gui.repository

//import androidx.paging.Pager
//import androidx.paging.PagingConfig
//import androidx.paging.PagingData
//import app.cash.paging.PagingData
//import app.cash.paging.Pager
//import app.cash.paging.PagingConfig
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.sqldelight.paging3.QueryPagingSource
import com.ghost.ollama.OllamaClient
import com.ghost.ollama.gui.EntityQueries
import com.ghost.ollama.gui.GetMessagesBySessionId
import com.ghost.ollama.gui.GetMessagesBySessionIdPaged
import com.ghost.ollama.gui.GetSessionsPaged
import com.ghost.ollama.models.chat.ChatMessage
import com.ghost.ollama.models.chat.ChatResponse
import com.ghost.ollama.models.generate.GenerateResponse
import com.ghost.ollama.models.modelMGMT.tags.ListModelsResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlin.time.Clock


class OllamaRepository(
    private val ollamaClient: OllamaClient,
    private val entityQueries: EntityQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ==========================================
    // SESSIONS MANAGEMENT
    // ==========================================

    /**
     * Creates a new chat or generate session.
     */
    fun createSession(title: String, sessionType: String = "CHAT"): String {
        val sessionId = generateUuid()
        val now = currentTimeMillis()

        entityQueries.insertSession(
            id = sessionId,
            title = title,
            created_at = now,
            updated_at = now,
            session_type = sessionType
        )
        return sessionId
    }

    /**
     * Deletes a session and cascades to delete all associated messages.
     */
    fun deleteSession(sessionId: String) {
        entityQueries.deleteSession(sessionId)
    }

    /**
     * Returns a PagingData Flow of Sessions, ordered by latest updated.
     */
    fun getSessionsPaged(pageSize: Int = 20): Flow<PagingData<GetSessionsPaged>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = entityQueries.countSessions(),
                    transacter = entityQueries,
                    context = ioDispatcher,
                    queryProvider = { limit, offset ->
                        entityQueries.getSessionsPaged(limit, offset)
                    }
                )
            }
        ).flow
    }

    // ==========================================
    // MESSAGE HISTORY (PAGING)
    // ==========================================

    /**
     * Returns a PagingData Flow of Messages for a specific session.
     * Useful for lazy-loading very long chat histories in the UI.
     */
    fun getMessagesPaged(sessionId: String, pageSize: Int = 30): Flow<PagingData<GetMessagesBySessionIdPaged>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = entityQueries.countMessages(sessionId),
                    transacter = entityQueries,
                    context = ioDispatcher,
                    queryProvider = { limit, offset ->
                        entityQueries.getMessagesBySessionIdPaged(sessionId, limit, offset)
                    }
                )
            }
        ).flow
    }

    // ==========================================
    // CHAT & GENERATION (API + DB SYNC)
    // ==========================================

    /**
     * Sends a chat message, stores it in the DB, builds history context,
     * and streams the Assistant's response while progressively updating the DB.
     */
    fun sendChatMessageStreaming(
        sessionId: String,
        modelName: String,
        content: String,
        role: ChatMessage.Role = ChatMessage.Role.USER
    ): Flow<ChatResponse> {
        val now = currentTimeMillis()

        // 1. Save User's message to DB
        entityQueries.insertMessage(
            id = generateUuid(),
            session_id = sessionId,
            model_name = modelName,
            role = role.name.lowercase(),
            content = content,
            thinking = null,
            images = null,
            tool_calls = null,
            logprobs = null,
            created_at = now,
            is_done = true
        )
        entityQueries.updateSessionTimestamp(now, sessionId)

        // 2. Build the context history for Ollama from the DB
        val history = entityQueries.getMessagesBySessionId(sessionId).executeAsList().map { it.toApiChatMessage() }

        // 3. Create an empty Assistant placeholder in DB for streaming
        val assistantMsgId = generateUuid()
        entityQueries.insertMessage(
            id = assistantMsgId,
            session_id = sessionId,
            model_name = modelName,
            role = ChatMessage.Role.ASSISTANT.name.lowercase(),
            content = "",
            thinking = null,
            images = null,
            tool_calls = null,
            logprobs = null,
            created_at = currentTimeMillis(),
            is_done = false
        )

        // 4. Trigger Network Request & Sync Stream chunks to DB
        return ollamaClient.chatStream(model = modelName, messages = history)
            .onEach { responseChunk ->
                // Fast SQLite String concatenation for tokens
                if (responseChunk.message.content.isNotEmpty()) {
                    entityQueries.appendMessageContent(responseChunk.message.content, assistantMsgId)
                }

                // DeepSeek R1 thought blocks
                if (!responseChunk.message.thinking.isNullOrEmpty() && responseChunk.message.thinking != "null") {
                    entityQueries.appendMessageThinking(responseChunk.message.thinking!!, assistantMsgId)
                }

                // Finalize metrics when streaming completes
                if (responseChunk.done) {
                    entityQueries.finalizeMessage(
                        doneReason = responseChunk.doneReason,
                        totalDuration = responseChunk.totalDuration,
                        loadDuration = responseChunk.loadDuration,
                        promptEvalCount = responseChunk.promptEvalCount?.toLong(),
                        promptEvalDuration = responseChunk.promptEvalDuration,
                        evalCount = responseChunk.evalCount?.toLong(),
                        evalDuration = responseChunk.evalDuration,
                        id = assistantMsgId
                    )
                    entityQueries.updateSessionTimestamp(currentTimeMillis(), sessionId)
                }
            }
            .flowOn(ioDispatcher) // Ensure network/DB runs off Main thread
    }

    /**
     * Sends a Quick Generate Request. Since generate doesn't use context history,
     * it just logs the prompt and streams the response to the DB.
     */
    fun sendGenerateStreaming(
        sessionId: String,
        modelName: String,
        prompt: String
    ): Flow<GenerateResponse> {
        val now = currentTimeMillis()

        // 1. Log the Prompt as a User Message
        entityQueries.insertMessage(
            id = generateUuid(), session_id = sessionId, model_name = modelName,
            role = "user", content = prompt, thinking = null, images = null,
            tool_calls = null, logprobs = null, created_at = now, is_done = true
        )

        // 2. Create Assistant Placeholder
        val assistantMsgId = generateUuid()
        entityQueries.insertMessage(
            id = assistantMsgId, session_id = sessionId, model_name = modelName,
            role = "assistant", content = "", thinking = null, images = null,
            tool_calls = null, logprobs = null, created_at = currentTimeMillis(), is_done = false
        )

        // 3. Stream & Sync
        return ollamaClient.generateStream(model = modelName, prompt = prompt)
            .onEach { responseChunk ->
                if (!responseChunk.response.isNullOrEmpty()) {
                    entityQueries.appendMessageContent(responseChunk.response!!, assistantMsgId)
                }
                if (!responseChunk.thinking.isNullOrEmpty()) {
                    entityQueries.appendMessageThinking(responseChunk.thinking!!, assistantMsgId)
                }
                if (responseChunk.done) {
                    entityQueries.finalizeMessage(
                        doneReason = responseChunk.doneReason,
                        totalDuration = responseChunk.totalDuration,
                        loadDuration = responseChunk.loadDuration,
                        promptEvalCount = responseChunk.promptEvalCount?.toLong(),
                        promptEvalDuration = responseChunk.promptEvalDuration,
                        evalCount = responseChunk.evalCount?.toLong(),
                        evalDuration = responseChunk.evalDuration,
                        id = assistantMsgId
                    )
                    entityQueries.updateSessionTimestamp(currentTimeMillis(), sessionId)
                }
            }.flowOn(ioDispatcher)
    }

    // ==========================================
    // MODEL MANAGEMENT (Passthrough to Client)
    // ==========================================

    suspend fun getAvailableModels(): ListModelsResponse {
        return ollamaClient.listModels()
    }


    /**
     * Renames an existing session.
     */
    fun renameSession(sessionId: String, newTitle: String) {
        entityQueries.updateSessionTitle(
            title = newTitle,
            updatedAt = currentTimeMillis(),
            id = sessionId
        )
    }

    /**
     * Deletes multiple sessions in a batch.
     */
    fun batchDeleteSessions(sessionIds: Set<String>) {
        entityQueries.transaction {
            sessionIds.forEach { id ->
                entityQueries.deleteSession(id)
            }
        }
    }

    /**
     * Returns a PagingData Flow of Sessions filtered by a search query.
     */
    fun searchSessionsPaged(query: String, pageSize: Int = 20): Flow<PagingData<GetSessionsPaged>> {
        return Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = {
                QueryPagingSource(
                    countQuery = entityQueries.countSearchSessions(query),
                    transacter = entityQueries,
                    context = ioDispatcher,
                    queryProvider = { limit, offset ->
                        // Pass the GetSessionsPaged constructor as the mapper!
                        entityQueries.searchSessionsPaged(
                            query = query,
                            limit = limit,
                            offset = offset,
                            mapper = ::GetSessionsPaged
                        )
                    }
                )
            }
        ).flow
    }

    /**
     * Exports a session's entire message history as a formatted JSON string.
     */
    fun exportSessionAsJson(sessionId: String): String {
        val session = entityQueries.getAllSessions().executeAsList().find { it.id == sessionId }
            ?: throw Exception("Session not found")

        val messages = entityQueries.getMessagesBySessionId(sessionId).executeAsList()

        // Wrap it in a simple map/object for clean JSON structure
        val exportData = mapOf(
            "session_id" to session.id,
            "title" to session.title,
            "created_at" to session.createdAt.toString(),
            "messages" to messages.map {
                mapOf(
                    "role" to it.role,
                    "content" to it.content,
                    "model" to it.modelName,
                    "created_at" to it.createdAt.toString()
                )
            }
        )

        val json = Json { prettyPrint = true }
        return json.encodeToString(exportData)
    }


    // ==========================================
    // UTILS & MAPPERS
    // ==========================================

    /** Maps a DB Entity to API Request Model for Context History */
    private fun GetMessagesBySessionId.toApiChatMessage(): ChatMessage {
        val mappedRole = ChatMessage.Role.entries.firstOrNull {
            it.name.equals(this.role, ignoreCase = true)
        } ?: ChatMessage.Role.USER

        // Note: You can use kotlinx.serialization to decode `images` or `tool_calls` strings back to objects if you need them in history
        return ChatMessage(
            role = mappedRole,
            content = this.content ?: "",
            thinking = this.thinking
        )
    }

    /** Simple multiplatform currentTime provider */
    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /** Simple multiplatform UUID Generator wrapper */
    private fun generateUuid(): String {
        // If you're on Kotlin 2.0.20+, use kotlin.uuid.Uuid.random().toString()
        // If not, replace this with your project's multiplatform UUID implementation
        // Example: com.benasher44.uuid.uuid4().toString()
        return "msg_${kotlin.random.Random.nextLong()}_${currentTimeMillis()}"
    }
}