package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ghost.ollama.gui.ui.components.InputBar
import com.ghost.ollama.gui.ui.components.InputBarState
import com.ghost.ollama.gui.ui.components.MessageBubble
import com.ghost.ollama.gui.ui.viewmodel.ChatMessage

@Composable
fun ChatContentScreen(
    messages: List<ChatMessage>,
    inputBarState: InputBarState,
    isMobile: Boolean,
    onMenuClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()

    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = if (isMobile) 72.dp else 16.dp,
                bottom = 296.dp // space for input bar
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onCopy = { onCopyMessage(message.id) },
                    onDelete = { onDeleteMessage(message.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Fade Overlay
        BottomFadeOverlay()

        // Floating Input Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(8.dp)
        ) {
            InputBar(
                modifier = Modifier.padding(
                    if (isMobile) 4.dp else 40.dp
                ).widthIn(max = 800.dp),
                state = inputBarState,
                onInputChanged = onInputChange,
                onSendClick = onSendClick,
                onAddClick = {},
                onToolsClick = {},
                onMicClick = {},
                onStopClick = {},
                onModelClick = { },
            )
        }
    }
}


@Composable()
private fun BoxScope.BottomFadeOverlay() {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(64.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    )
}