package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.LazyPagingItems
import com.ghost.ollama.gui.ui.components.ChatScrollbar
import com.ghost.ollama.gui.ui.components.InputBar
import com.ghost.ollama.gui.ui.components.InputBarState
import com.ghost.ollama.gui.ui.components.MessageBubble
import com.ghost.ollama.gui.viewmodel.ChatEvent
import com.ghost.ollama.gui.viewmodel.UiChatMessage

@Composable
fun ChatContentScreen(
    messages: LazyPagingItems<UiChatMessage>,
    inputBarState: InputBarState,
    isMobile: Boolean,
    onInputChange: (String) -> Unit,
    onChatEvent: (ChatEvent) -> Unit,
    onToolsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()


    Box(
        modifier = modifier
            .fillMaxSize()

    ) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
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
            items(
                count = messages.itemCount,
                key = { index -> messages[index]?.id ?: index }
            ) { index ->

                val reversedIndex = messages.itemCount - 1 - index
                val messageItem = messages[reversedIndex]

                messageItem?.let { message ->
                    MessageBubble(
                        message = message,
                        onCopy = { onChatEvent(ChatEvent.CopyMessage(message.id)) },
                        onDelete = { onChatEvent(ChatEvent.DeleteMessage(message.id)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        }

        ChatScrollbar(
            listState = listState,
            reverseLayout = true,
            modifier = Modifier.padding(end = 4.dp)
        )

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
                onSendClick = { onChatEvent(ChatEvent.SendMessage(inputBarState.inputText)) },
                onAddClick = {},
                onToolsClick = onToolsClicked,
                onMicClick = {},
                onStopClick = { onChatEvent(ChatEvent.StopGeneration) },
                onModelSelected = { onChatEvent(ChatEvent.SelectModel(it)) },
                onRetryModel = { /* Handle retry model if needed */ }
            )
        }
    }


    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.itemCount) {
        if (messages.itemCount > 0) {
            listState.scrollToItem(messages.itemCount - 1)
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