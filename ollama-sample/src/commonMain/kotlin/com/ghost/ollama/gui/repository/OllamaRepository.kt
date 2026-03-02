package com.ghost.ollama.gui.repository

//import androidx.paging.Pager
//import androidx.paging.PagingConfig
//import androidx.paging.PagingData
//import app.cash.paging.PagingData
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import com.ghost.ollama.OllamaClient
import com.ghost.ollama.gui.EntityQueries
import com.ghost.ollama.gui.MessageView
import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.models.ModelDetailState
import com.ghost.ollama.gui.models.ModelsState
import com.ghost.ollama.gui.utils.toChatOptions
import com.ghost.ollama.gui.utils.toUiChatMessage
import com.ghost.ollama.gui.viewmodel.UiChatMessage
import com.ghost.ollama.models.chat.ChatMessage
import com.ghost.ollama.models.chat.ChatOptions
import com.ghost.ollama.models.chat.ChatResponse
import com.ghost.ollama.models.generate.GenerateResponse
import com.ghost.ollama.models.modelMGMT.tags.ListModelsResponse
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock


class OllamaRepository(
    private val ollamaClient: OllamaClient,
    private val entityQueries: EntityQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun ollamaVersion(): String {
        ChatOptions
        return ollamaClient.ollamaVersion().version
    }

    fun observerOllamaStatus(): Flow<Boolean> {
        return ollamaClient.observeOllamaStatus()
    }

    // ==========================================
    // SESSIONS MANAGEMENT
    // ==========================================

    init {
        recoverUnfinishedMessages()
    }

    private fun recoverUnfinishedMessages() {
        entityQueries.markAllUnfinishedAssistantMessagesAsError()
    }

    /**
     * Creates a new session.
     */
    suspend fun createOrReuseSession(
        globalSettings: GlobalSettings,
        title: String = "New Chat",
        sessionType: String = "CHAT",
    ): String {

        val latestSession = entityQueries.getLatestSession().executeAsOneOrNull()

        if (latestSession != null) {
            val messageCount = entityQueries
                .countMessages(latestSession.id)
                .executeAsOne()

            if (messageCount == 0L) {
                return latestSession.id
            }
        }

        val defaults = globalSettings.defaultChatOptions

        val sessionId = generateUuid()
        val now = currentTimeMillis()

        entityQueries.insertSession(
            id = sessionId,
            title = title,
            created_at = now,
            updated_at = now,
            pinned = false,
            session_type = sessionType,

            seed = defaults.seed?.toLong(),
            temperature = defaults.temperature?.toDouble(),
            top_k = defaults.topK?.toLong(),
            top_p = defaults.topP?.toDouble(),
            min_p = defaults.minP?.toDouble(),
            stop = defaults.stop,
            num_ctx = defaults.numCtx?.toLong(),
            num_predict = defaults.numPredict?.toLong(),
            format = defaults.format?.name
        )

        return sessionId
    }


    fun getLastUsedSession(): Flow<SessionView?> {
        return entityQueries
            .getLastUsedSession()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
    }

    fun sessionExists(sessionId: String): Boolean {
        return entityQueries.sessionExists(sessionId).executeAsOne()
    }

    fun clearSession(sessionId: String) {
        entityQueries.clearSession(sessionId)
    }

    fun updateSession(session: SessionView) {
        entityQueries.updateSession(
            title = session.title,
            pinned = session.pinned,
            updatedAt = currentTimeMillis(),
            seed = session.seed,
            temperature = session.temperature,
            topK = session.topK,
            topP = session.topP,
            minP = session.minP,
            stop = session.stop,
            numCtx = session.numCtx,
            numPredict = session.numPredict,
            format = session.format,
            id = session.id
        )
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun getOrCreateActiveSession(
        globalSettings: GlobalSettings,
        defaultTitle: String = "New Chat",
        sessionType: String = "CHAT"
    ): Flow<SessionView> {

        return entityQueries
            .getLastUsedSession()
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .flatMapLatest { session ->

                if (session != null) {
                    flowOf(session)
                } else {
                    flow {
                        val newId = createOrReuseSession(globalSettings, defaultTitle, sessionType)
                        val newSession = entityQueries
                            .getSessionById(newId)
                            .executeAsOne()

                        emit(newSession)
                    }
                }
            }
    }


    suspend fun getOrCreateActiveSessionOnce(
        globalSettings: GlobalSettings,
        defaultTitle: String = "New Chat",
        sessionType: String = "CHAT"
    ): SessionView = withContext(ioDispatcher) {

        val session = entityQueries
            .getLastUsedSession()
            .executeAsOneOrNull()

        if (session != null) {
            return@withContext session
        }

        val newId = createOrReuseSession(
            globalSettings = globalSettings,
            title = defaultTitle,
            sessionType = sessionType
        )

        entityQueries
            .getSessionById(newId)
            .executeAsOne()
    }


    fun toggleSessionPin(sessionId: String) {
        entityQueries.toggleSessionPin(currentTimeMillis(), sessionId)
    }

    fun getSessionById(sessionId: String): Flow<SessionView?> {
        return entityQueries
            .getSessionById(sessionId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
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
    fun getSessionsPaged(pageSize: Int = 20): Flow<PagingData<SessionView>> {
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


    fun getMessageById(messageId: String): MessageView? {
        return entityQueries.getMessageById(messageId).executeAsOneOrNull()
    }

    suspend fun updateMessageState(messageId: String, isDone: Boolean, doneReason: String) {
        entityQueries.updateMessageDoneStatus(
            id = messageId,
            isDone = isDone,
            doneReason = doneReason,
        )
    }

    suspend fun updateMessageError(messageId: String, error: String, isError: Boolean) {
        entityQueries.updateMessageErrorStatus(
            id = messageId,
            errored = isError,
            errorMessage = error
        )
    }

    fun observeSessionExists(sessionId: String): Flow<Boolean> {
        return entityQueries
            .getSessionById(sessionId)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it != null }
    }

    /**
     * Returns a PagingData Flow of Messages for a specific session.
     * Useful for lazy-loading very long chat histories in the UI.
     */
    fun getMessagesPaged(sessionId: String, pageSize: Int = 30): Flow<PagingData<MessageView>> {
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
        session: SessionView,
        modelName: String,
        content: String,
        role: ChatMessage.Role = ChatMessage.Role.USER
    ): Flow<ChatResponse> {
        val now = currentTimeMillis()


        val messageCountBefore = entityQueries
            .countMessages(session.id)
            .executeAsOne()

        val isFirstMessage = messageCountBefore == 0L


        // 1. Save User's message to DB
        entityQueries.insertMessage(
            id = generateUuid(),
            session_id = session.id,
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

        // 🔥 1.5 If first message → update session title
        if (isFirstMessage) {
            val trimmedTitle = content
                .trim()
                .replace("\n", " ")
                .take(60) // limit length

            entityQueries.updateSessionTitle(
                title = trimmedTitle.ifBlank { "New Chat" },
                updatedAt = now,
                id = session.id,
            )
        }


        entityQueries.updateSessionTimestamp(now, session.id)

        // 2. Build the context history for Ollama from the DB
        val history = entityQueries.getMessagesBySessionId(session.id).executeAsList().map { it.toApiChatMessage() }

        // 3. Create an empty Assistant placeholder in DB for streaming
        val assistantMsgId = generateUuid()
        entityQueries.insertMessage(
            id = assistantMsgId,
            session_id = session.id,
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

        val options = session.toChatOptions()

        // 4. Trigger Network Request & Sync Stream chunks to DB
        return ollamaClient.chatStream(
            model = modelName,
            messages = history,
            options = options
        )

            .onEach { responseChunk ->
//                delay(50) // Simulate network delay for testing streaming in UI
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
                    entityQueries.updateSessionTimestamp(currentTimeMillis(), session.id)
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
    fun searchSessionsPaged(query: String, pageSize: Int = 20): Flow<PagingData<SessionView>> {
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

    fun deleteChatMessage(messageId: String) {
        entityQueries.deleteMessageById(messageId)
    }

    // Extension for your repository if needed
    suspend fun getLastUserMessage(sessionId: String): UiChatMessage? {
        // Implement based on your data layer
        return entityQueries.getLastMessageOfSessionId(sessionId).executeAsOneOrNull()?.toUiChatMessage(sessionId)

    }

    fun getLastMessage(): MessageView? {
        return entityQueries.getLastMessage().executeAsOneOrNull()
    }

    // Models
    fun observeModels(): Flow<ModelsState> {
        return ollamaClient.observeModels()
            .map<ListModelsResponse?, ModelsState> { response ->
                Napier.d { ("Received models update: ${response?.models?.joinToString { it.name }}") }
                if (response != null) {
                    ModelsState.Success(response)
                } else {
                    ModelsState.Error("No response from Ollama")
                }
            }
            .onStart {
                emit(ModelsState.Loading)
            }
            .catch { e ->
                emit(
                    ModelsState.Error(
                        e.message ?: "Failed to fetch models"
                    )
                )
            }
    }

    fun getModelDetail(
        name: String,
        verbose: Boolean = false
    ): Flow<ModelDetailState> = flow {
        Napier.d { ("Received model detail update: ${name}") }

        emit(ModelDetailState.Loading)

        val response = ollamaClient.showModel(name, verbose)

        Napier.d { ("Fetched model details for $name: $response") }

        emit(ModelDetailState.Success(response))

    }
        .catch { e ->
            Napier.e(e) { ("Error fetching model details for $name: ${e.message}") }
            emit(
                ModelDetailState.Error(
                    e.message ?: "Failed to fetch model details"
                )
            )
        }
        .flowOn(Dispatchers.IO)


    // ==========================================
    // UTILS & MAPPERS
    // ==========================================

    /** Maps a DB Entity to API Request Model for Context History */
    private fun MessageView.toApiChatMessage(): ChatMessage {
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

