package com.ghost.ollama.gui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ghost.ollama.gui.ui.theme.AppTheme
import com.ghost.ollama.gui.ui.viewmodel.ChatMessage
import com.ghost.ollama.gui.ui.viewmodel.MessageState
import com.mikepenz.markdown.m3.Markdown
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Clock
import com.ghost.ollama.models.chat.ChatMessage.Role as MessageRole

/**
 * A modern chat message bubble with role icons, markdown, timestamps,
 * expandable thinking, and detailed generation info.
 */
/**
 * A modern chat message bubble with role icons, timestamps,
 * expandable thinking, and detailed generation info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBubbleOLd(
    message: ChatMessage, onCopy: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier
) {
    val message = message.copy(thinking = "ya i am thinking", totalDuration = 100L)
    val isUser = message.role == MessageRole.USER
    val isDone = message.state is MessageState.Done
    val hasThinking = !message.thinking.isNullOrBlank() && !isUser
    val hasDetails =
        message.totalDuration != null || message.loadDuration != null || message.promptEvalCount != null || message.evalCount != null

    // Local expansion states
    var showThinking by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    Column {
        Row {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 8.dp).size(32.dp)
                ) {
                    Icon(
                        painterResource(Res.drawable.smart_toy),
                        contentDescription = "Assistant",
                        modifier = Modifier.padding(6.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                if (hasThinking) {
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { showThinking = !showThinking },
                        modifier = Modifier,
                    ) {
                        Text("Show Thinking")
                        Icon(
                            painterResource(Res.drawable.keyboard_arrow_down),
                            contentDescription = "Toggle thinking",
                            tint = if (showThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.7f
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showThinking && hasThinking,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Thinking",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Markdown(
                                content = message.thinking ?: "",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                when (val msgState = message.state) {
                    is MessageState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    is MessageState.Generating -> {
                        if (msgState.isThinking) {
                            Text(
                                "Thinking...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        // Using standard Text here. Replace with Markdown() if available.
                        Text(
                            text = message.content ?: "", modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is MessageState.Errored -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Error: ${msgState.error}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    else -> { // Idle / Done
                        if (!message.content.isNullOrBlank()) {
                            Markdown(
                                content = message.content, modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                "(Empty)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

        }

        if (isDone) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isUser) 0.dp else 40.dp, // Align with bubble start (32 + 8)
                        end = if (isUser) 4.dp else 0.dp,
                        top = 4.dp
                    ),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timestamp
                Text(
                    text = formatTimestamp(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Actions available when generation is fully complete
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(
                        painterResource(Res.drawable.content_copy),
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        painterResource(Res.drawable.delete_forever),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Info button (if details exist)
                if (hasDetails) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painterResource(Res.drawable.info),
                            contentDescription = "Generation info",
                            tint = if (showDetails) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.7f
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Thinking dropdown button (if thinking present)
            }
        }

        AnimatedVisibility(
            visible = showDetails && hasDetails,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            GenerationDetailsCard(
                message = message,
                modifier = Modifier
                    .padding(
                        start = if (isUser) 0.dp else 40.dp,
                        end = if (isUser) 4.dp else 0.dp,
                        top = 8.dp
                    )
                    .fillMaxWidth(0.85f)
            )
        }
    }

}


@Composable
fun MessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        MessageRole.USER -> UserMessageBubble(
            message = message,
            onCopy = onCopy,
            onDelete = onDelete,
            modifier = modifier
        )

        else -> AssistantMessageBubble(
            message = message,
            onCopy = onCopy,
            onDelete = onDelete,
            modifier = modifier
        )
    }
}


@Composable
fun UserMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {

    val isDone = message.state is MessageState.Done
    var expanded by remember { mutableStateOf(false) }

//    LocalViewConfiguration.current.


    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        BoxWithConstraints {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp
                ),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = maxWidth * 0.85f)
                    .wrapContentWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {

                    ExpandableText(
                        text = message.content ?: "",
                        expanded = expanded,
                        onToggle = { expanded = !expanded }
                    )

                    if (isDone) {
                        Spacer(Modifier.height(6.dp))
                        MessageMetaRow(
                            message = message,
                            onCopy = onCopy,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun ExpandableText(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    collapsedMaxLines: Int = 4
) {
    var isOverflowing by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End) {

        Text(
            text = text,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            onTextLayout = { result ->
                if (!expanded) {
                    isOverflowing = result.hasVisualOverflow
                }
            }
        )

        if (isOverflowing || expanded) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (expanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onToggle() }
            )
        }
    }
}


@Composable
fun AssistantMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = message.copy(thinking = "ya i am thinking", totalDuration = 100L)
    val isDone = message.state is MessageState.Done
    val hasThinking = !message.thinking.isNullOrBlank()
    val hasDetails =
        message.totalDuration != null ||
                message.loadDuration != null ||
                message.promptEvalCount != null ||
                message.evalCount != null

    var showThinking by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    val rotateAnimation = animateFloatAsState(
        targetValue = if (showThinking) 180f else 0f,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .padding(top = 6.dp)
                .size(32.dp)
        ) {
            Icon(
                painterResource(Res.drawable.smart_toy),
                contentDescription = "Assistant",
                modifier = Modifier.padding(6.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {

            // Thinking toggle
            if (hasThinking) {
                TextButton(onClick = { showThinking = !showThinking }) {
                    Text("Show Thinking")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painterResource(Res.drawable.keyboard_arrow_down),
                        contentDescription = "Toggle thinking",
                        tint = if (showThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.7f
                        ),
                        modifier = Modifier.size(18.dp).rotate(rotateAnimation.value)
                    )
                }
            }

            AnimatedVisibility(showThinking && hasThinking) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(0.85f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Markdown(
                            content = message.thinking ?: "",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                }

            }

            MessageContent(message)

            if (isDone) {
                Spacer(Modifier.height(6.dp))
                MessageMetaRow(
                    message = message,
                    onCopy = onCopy,
                    onDelete = onDelete,
                    showDetails = showDetails,
                    onToggleDetails = { showDetails = !showDetails },
                    hasDetails = hasDetails
                )
            }

            AnimatedVisibility(showDetails && hasDetails) {
                GenerationDetailsCard(
                    message = message,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MessageContent(message: ChatMessage) {
    when (val state = message.state) {

        is MessageState.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Loading...")
            }
        }

        is MessageState.Generating -> {
            Markdown(
                content = message.content ?: "",
                modifier = Modifier.fillMaxWidth()
            )
        }

        is MessageState.Errored -> {
            Text(
                "Error: ${state.error}",
                color = MaterialTheme.colorScheme.error
            )
        }

        else -> {
            Markdown(
                content = message.content ?: "(Empty)",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MessageMetaRow(
    message: ChatMessage,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    showDetails: Boolean = false,
    onToggleDetails: (() -> Unit)? = null,
    hasDetails: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            formatTimestamp(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
            Icon(
                painterResource(Res.drawable.content_copy),
                contentDescription = "Copy",
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                painterResource(Res.drawable.delete_forever),
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp)
            )
        }

        if (hasDetails && onToggleDetails != null) {
            IconButton(onClick = onToggleDetails, modifier = Modifier.size(24.dp)) {
                Icon(
                    painterResource(Res.drawable.info),
                    contentDescription = "Details",
                    tint = if (showDetails)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


//    Column(
//        modifier = modifier.fillMaxWidth(),
//        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
//    ) {
//        // Main row: icon (if any) + bubble
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.Top,
//            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
//        ) {
//            // Icon for non‑user roles
//
//
//            // Message bubble with wrapping but restricted maximum width constraint
//            Surface(
//                shape = RoundedCornerShape(
//                    topStart = 16.dp,
//                    topEnd = 16.dp,
//                    bottomStart = if (isUser) 16.dp else 4.dp,
//                    bottomEnd = if (isUser) 4.dp else 16.dp
//                ),
//                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
//                else MaterialTheme.colorScheme.surfaceVariant,
//                tonalElevation = 1.dp,
//                modifier = Modifier.weight(0.85f, fill = false)
//            ) {
//                Column(
//                    modifier = Modifier.padding(12.dp)
//                ) {
//                    // Message state handling
//
//                }
//            }
//        }
//
//        // Metadata row: timestamp, copy, delete, info button, thinking dropdown
//
//
//        // Expandable thinking section
//
//
//        // Expandable generation details
//
//    }


/**
 * Displays detailed generation metrics in a card.
 */
@Composable
private fun GenerationDetailsCard(
    message: ChatMessage, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier, colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Generation Details",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))

            val details = listOfNotNull(
                "Total duration" to message.totalDuration?.let { "${it}ms" },
                "Load duration" to message.loadDuration?.let { "${it}ms" },
                "Prompt eval count" to message.promptEvalCount?.toString(),
                "Prompt eval duration" to message.promptEvalDuration?.let { "${it}ms" },
                "Eval count" to message.evalCount?.toString(),
                "Eval duration" to message.evalDuration?.let { "${it}ms" },
                "Done reason" to message.doneReason
            )

            details.forEachIndexed { index, (label, value) ->
                if (value != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (index < details.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats a timestamp (epoch millis) to a readable time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    return try {
        val date = Date(timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        ""
    }
}

// Example usage of kotlinx.datetime alternative (commented):
// @OptIn(kotlinx.datetime.ExperimentalDateTimeFormat::class)
// private fun formatTimestamp(timestamp: Long): String {
//     val instant = Clock.System.now() // or from epoch
//     val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
//     return dateTime.format(DateTimeFormat { hour(); minute() })
// }


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MessageBubblePreview() {
    var showPreviewDialog by remember { mutableStateOf(true) }
    var messages by remember { mutableStateOf(sampleMessages) }

    if (showPreviewDialog) {
        Dialog(
            onDismissRequest = { showPreviewDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().height(700.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    // Header
                    Surface(
                        tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chat Message Preview", style = MaterialTheme.typography.headlineSmall
                            )
                            IconButton(onClick = { showPreviewDialog = false }) {
                                Icon(painter = painterResource(Res.drawable.close), contentDescription = "Close")
                            }
                        }
                    }

                    // Message List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message, onCopy = { /* Handle copy */ }, onDelete = {
                                    messages = messages.filter { it.id != message.id }
                                }, modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Sample messages for preview
private val sampleMessages = listOf(
    // System message
    ChatMessage(
        id = "1",
        role = MessageRole.SYSTEM,
        content = "Welcome to the chat! This is a system message with **markdown** support.\n\n- Feature 1\n- Feature 2\n- Feature 3",
        thinking = "System initialization complete. All modules loaded successfully.",
        state = MessageState.Done,
        createdAt = Clock.System.now().toEpochMilliseconds() - 3600000
    ),

    // User message
    ChatMessage(
        id = "2",
        role = MessageRole.USER,
        content = "Can you explain quantum computing in simple terms? I'd like to understand the basics.",
        state = MessageState.Done,
        createdAt = Clock.System.now().toEpochMilliseconds() - 1800000
    ),

    // Assistant message with thinking and details
    ChatMessage(
        id = "3",
        role = MessageRole.ASSISTANT,
        content = """
            # Quantum Computing Basics
            
            Quantum computing is a fascinating field that leverages **quantum mechanics** to process information.
            
            ## Key Concepts:
            1. **Qubits**: Unlike classical bits (0 or 1), qubits can exist in superposition
            2. **Superposition**: A qubit can be both 0 and 1 simultaneously
            3. **Entanglement**: Qubits can be correlated in ways impossible in classical physics
            
            Here's a simple code example:
            ```python
            from qiskit import QuantumCircuit
            
            # Create a quantum circuit with 1 qubit
            circuit = QuantumCircuit(1, 1)
            circuit.h(0)  # Apply Hadamard gate for superposition
            circuit.measure(0, 0)
            ```
            
            This is just scratching the surface! Would you like me to elaborate on any specific aspect?
        """.trimIndent(),
        thinking = """
            The user wants a simple explanation of quantum computing. I should:
            1. Start with a basic analogy
            2. Explain superposition without math
            3. Give practical example with code
            4. Offer to dive deeper
            
            Using the coin flip analogy for superposition might work well.
        """.trimIndent(),
        state = MessageState.Done,
        totalDuration = 2450,
        loadDuration = 120,
        promptEvalCount = 156,
        promptEvalDuration = 45,
        evalCount = 89,
        evalDuration = 38,
        doneReason = "stop",
        createdAt = Clock.System.now().toEpochMilliseconds() - 900000
    ),

    // Tool message
    ChatMessage(
        id = "4", role = MessageRole.TOOL, content = """
            ```json
            {
              "function": "search_knowledge_base",
              "parameters": {
                "query": "quantum computing basics",
                "max_results": 5
              },
              "result": {
                "total_found": 42,
                "top_matches": [
                  {"title": "Introduction to Quantum Computing", "relevance": 0.95},
                  {"title": "Qubits Explained", "relevance": 0.92}
                ]
              }
            }
            ```
        """.trimIndent(), state = MessageState.Done, createdAt = Clock.System.now().toEpochMilliseconds() - 800000
    ),

    // Assistant message with loading state
    ChatMessage(
        id = "5",
        role = MessageRole.ASSISTANT,
        content = "Generating response...",
        state = MessageState.Loading,
        createdAt = Clock.System.now().toEpochMilliseconds() - 600000
    ),

    // Assistant message with generating state
    ChatMessage(
        id = "6",
        role = MessageRole.ASSISTANT,
        content = "I'm thinking about how to best explain quantum entanglement...",
        state = MessageState.Generating(isThinking = true),
        createdAt = Clock.System.now().toEpochMilliseconds() - 300000
    ),

    // User message with error state
    ChatMessage(
        id = "7",
        role = MessageRole.USER,
        content = "Show me something complex",
        state = MessageState.Errored(error = "Failed to process request: API timeout after 30 seconds"),
        createdAt = Clock.System.now().toEpochMilliseconds() - 120000
    ),

    // Assistant with long content and multiple details
    ChatMessage(
        id = "8",
        role = MessageRole.ASSISTANT,
        content = """
            # Quantum Entanglement Explained
            
            Quantum entanglement is one of the most fascinating phenomena in quantum mechanics. When two particles become entangled, their quantum states become linked in such a way that measuring one instantly affects the other, regardless of the distance between them.
            
            ## The EPR Paradox
            Einstein famously called this "spooky action at a distance." Along with Podolsky and Rosen, he proposed the EPR paradox to challenge the completeness of quantum mechanics.
            
            ## Real-world Applications
            - **Quantum Cryptography**: Secure communication using entangled photons
            - **Quantum Teleportation**: Transferring quantum states between locations
            - **Quantum Computing**: Entangled qubits for parallel processing
            
            ## Mathematical Description
            The Bell state is a maximally entangled quantum state:
            
            ```
            |Φ⁺⟩ = (|00⟩ + |11⟩)/√2
            ```
            
            Would you like to explore any of these topics in more detail?
        """.trimIndent(),
        thinking = """
            The user seems interested in quantum mechanics. I should:
            1. Explain entanglement intuitively
            2. Connect to historical context (EPR paradox)
            3. Show practical applications
            4. Include mathematical representation for completeness
            5. End with engagement question
        """.trimIndent(),
        state = MessageState.Done,
        totalDuration = 5120,
        loadDuration = 230,
        promptEvalCount = 423,
        promptEvalDuration = 156,
        evalCount = 267,
        evalDuration = 189,
        doneReason = "stop",
        createdAt = Clock.System.now().toEpochMilliseconds() - 60000
    )
)

// Preview wrapper with theme
@Composable
@Preview
fun ThemedMessageBubblePreview() {
    MaterialTheme {
        MessageBubblePreview()
    }
}

// Dark theme variant
@Composable
@Preview()
fun DarkThemedMessageBubblePreview() {
    AppTheme(true) {
        MessageBubblePreview()
    }
}