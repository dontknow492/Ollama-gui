package com.ghost.ollama.gui.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.painterResource


@Immutable
data class InputBarState(
    val inputText: String = "",
    val placeholder: String = "Ask anything or share a file",
    val isGenerating: Boolean = false,
    val selectedModel: String = "Fast",
    val isSendEnabled: Boolean = false
)


//@Composable
@Composable
fun InputBar(
    state: InputBarState,
    onInputChanged: (String) -> Unit,
    onAddClick: () -> Unit,
    onToolsClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onStopClick: () -> Unit,
    onModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp)
        ) {

            InputTextField(
                text = state.inputText,
                placeholder = state.placeholder,
                enabled = !state.isGenerating,
                scrollState = scrollState,
                focusRequester = focusRequester,
                onTextChanged = onInputChanged,
                onSend = {
                    if (state.inputText.isNotBlank()) {
                        onSendClick(state.inputText.trim())
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            BottomActionRow(
                state = state,
                onAddClick = onAddClick,
                onToolsClick = onToolsClick,
                onMicClick = onMicClick,
                onSendClick = onSendClick,
                onStopClick = onStopClick,
                onModelClick = onModelClick
            )
        }
    }
}

@Composable
private fun InputTextField(
    text: String,
    placeholder: String,
    enabled: Boolean,
    scrollState: ScrollState,
    focusRequester: FocusRequester,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = text,
        onValueChange = onTextChanged,
        enabled = enabled,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = {
                onSend()
                keyboardController?.hide()
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp, max = 180.dp) // 👈 MAX HEIGHT
            .verticalScroll(scrollState)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {

                    // Enter to send (no shift)
                    if (event.key == Key.Enter && !event.isShiftPressed) {
                        onSend()
                        return@onPreviewKeyEvent true
                    }

                    // Ctrl/Cmd + Enter = force send
                    if (event.key == Key.Enter && event.isCtrlPressed) {
                        onSend()
                        return@onPreviewKeyEvent true
                    }

                    // ESC clears input
                    if (event.key == Key.Escape) {
                        onTextChanged("")
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        decorationBox = { innerTextField ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Stable
@Composable
private fun BottomActionRow(
    state: InputBarState,
    onAddClick: () -> Unit,
    onToolsClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onStopClick: () -> Unit,
    onModelClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeftActions(
            onAddClick = onAddClick,
            onToolsClick = onToolsClick
        )

        RightActions(
            state = state,
            onMicClick = onMicClick,
            onSendClick = onSendClick,
            onStopClick = onStopClick,
            onModelClick = onModelClick
        )
    }
}

@Composable
private fun LeftActions(
    onAddClick: () -> Unit,
    onToolsClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onAddClick) {
            Icon(
                painter = painterResource(Res.drawable.add),
                contentDescription = "Add attachment"
            )
        }

        TextButton(
            onClick = onToolsClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.tune),
                contentDescription = "Tools",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Tools")
        }
    }
}

@Composable
private fun RightActions(
    state: InputBarState,
    onMicClick: () -> Unit,
    onSendClick: (String) -> Unit,
    onStopClick: () -> Unit,
    onModelClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // Model selector
        TextButton(
            onClick = onModelClick,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(state.selectedModel)
            Icon(
                painter = painterResource(Res.drawable.keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(onClick = onMicClick) {
            Icon(
                painter = painterResource(Res.drawable.mic),
                contentDescription = "Microphone"
            )
        }

        when {
            state.isGenerating -> {
                IconButton(onClick = onStopClick) {
                    Icon(
                        painter = painterResource(Res.drawable.stop_circle),
                        contentDescription = "Stop Generating"
                    )
                }
            }

            state.isSendEnabled -> {
                IconButton(
                    onClick = { onSendClick(state.inputText) }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.send),
                        contentDescription = "Send Message"
                    )
                }
            }
        }
    }
}