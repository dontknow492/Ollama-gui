package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghost.ollama.gui.ui.components.InputBar
import com.ghost.ollama.gui.ui.components.InputBarState
import ollama_kmp.ollama_sample.generated.resources.Res
import ollama_kmp.ollama_sample.generated.resources.stars_2
import org.jetbrains.compose.resources.painterResource

// Data class for our custom suggestions
data class SuggestionPrompt(
    val emoji: String,
    val text: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptySessionScreen(
    modifier: Modifier = Modifier,
    userName: String = "Manish",
    inputBarState: InputBarState, // Using the state class from the previous step
    onInputChanged: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    // Callbacks to pass down to the GeminiInputBar
    onAddClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    onModelSelectClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onSendClick: (String) -> Unit = {},
    onStopClick: () -> Unit = {}
) {
    // Replacement prompts as requested
    val suggestions = listOf(
        SuggestionPrompt("💡", "Brainstorm ideas"),
        SuggestionPrompt("📝", "Write an email"),
        SuggestionPrompt("💻", "Code a feature"),
        SuggestionPrompt("📊", "Summarize a text"),
        SuggestionPrompt("🚀", "Boost my day"),
        SuggestionPrompt("🤔", "Explain a concept")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp) // Keeps it from stretching too wide on desktop/tablets
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Greeting section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.stars_2),
                    contentDescription = "Sparkles",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hi $userName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            // Main Question
            Text(
                text = "Where should we start?",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // The Input Bar (Reusing your previous component)
            InputBar(
                state = inputBarState,
                onInputChanged = onInputChanged,
                onAddClick = onAddClick,
                onToolsClick = onToolsClick,
                onMicClick = onMicClick,
                onSendClick = onSendClick,
                onModelClick = onModelSelectClick,
                onStopClick = onStopClick,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Suggestion Chips Grid
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionActionChip(
                        emoji = suggestion.emoji,
                        text = suggestion.text,
                        onClick = { onSuggestionClick(suggestion.text) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionActionChip(
    emoji: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // Slightly transparent matching the image style
        shape = CircleShape,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}