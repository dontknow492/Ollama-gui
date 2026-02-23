package com.ghost.ollama.gui.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
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

@Composable
fun InputBarOld(
    modifier: Modifier = Modifier,
    state: InputBarState,
    onInputChanged: (String) -> Unit,
    onAddClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onSendClick: (String) -> Unit = {},
    onStopClick: () -> Unit = {}
) {
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    // Dynamic surface matching the app's theme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant, // Dynamic background
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant // Dynamic text/icon color
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp)
        ) {
            // Text Input Area
            BasicTextField(
                value = state.inputText,
                onValueChange = onInputChanged,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp) // Gives it vertical space
                    .padding(horizontal = 4.dp),
                decorationBox = { innerTextField ->
                    if (state.inputText.isEmpty()) {
                        Text(
                            text = state.placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Add & Tools
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            painter = painterResource(Res.drawable.add),
                            contentDescription = "Add attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Tools Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToolsClick() }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Using Outline Tune as a close replacement for the custom Gemini tools icon
                        Icon(
                            painter = painterResource(Res.drawable.tune),
                            contentDescription = "Tools",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tools",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }

                // Right Side: Model Selector (Fast) & Mic/Send
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Model Selector Button & Dropdown
                    Box {
                        val modelName = "Fast"
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isModelDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = modelName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                            Icon(
                                painter = painterResource(Res.drawable.keyboard_arrow_down),
                                contentDescription = "Select Model",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(onClick = onMicClick) {
                        Icon(
                            painter = painterResource(Res.drawable.mic),
                            contentDescription = "Microphone",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Show Send button dynamically when input exists
                    when {
                        state.isGenerating -> {
                            IconButton(onClick = onStopClick) {
                                Icon(
                                    painter = painterResource(Res.drawable.stop_circle),
                                    contentDescription = "Stop Generating",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        state.isSendEnabled -> {
                            IconButton(onClick = { onSendClick(state.inputText) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.send),
                                    contentDescription = "Send Message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp)
        ) {

            InputTextField(
                text = state.inputText,
                placeholder = state.placeholder,
                onTextChanged = onInputChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

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
    onTextChanged: (String) -> Unit
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChanged,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 4.dp),
        decorationBox = { innerTextField ->
            if (text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
            innerTextField()
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