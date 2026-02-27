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
import androidx.compose.runtime.*
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
import com.ghost.ollama.gui.models.ModelDetailState
import com.ghost.ollama.gui.models.ModelsState
import com.ghost.ollama.models.modelMGMT.tags.ModelInfo
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource


@Immutable
data class InputBarState(
    val inputText: String = "",
    val placeholder: String = "Ask anything or share a file",
    val isGenerating: Boolean = false,
    val selectedModel: ModelDetailState,
    val isSendEnabled: Boolean = false,
    val installedModels: ModelsState,
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
    onModelSelected: (ModelInfo) -> Unit,
    onRetryModel: () -> Unit,
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
                onModelSelected = onModelSelected,
                onRetryModel = onRetryModel
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
    onModelSelected: (ModelInfo) -> Unit,
    onRetryModel: () -> Unit,
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
            onModelSelected = onModelSelected,
            onRetryModel = onRetryModel
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
    onModelSelected: (ModelInfo) -> Unit,
    onRetryModel: () -> Unit
) {
    var isModelMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // Model selector


        ModelSelector(
            selectedModelState = state.selectedModel,
            modelsState = state.installedModels,
            expanded = isModelMenuExpanded,
            onExpandChange = { expanded ->
                isModelMenuExpanded = expanded
            },
            onModelSelected = { model ->
                onModelSelected(model)
            },
            onRetry = onRetryModel
        )

        IconButton(onClick = onMicClick) {
            Icon(
                painter = painterResource(Res.drawable.mic),
                contentDescription = "Microphone"
            )
        }

        when {
            state.isGenerating -> {
                IconButton(
                    onClick = onStopClick,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.stop_circle),
                        contentDescription = "Stop Generating",
                    )
                }
            }

            state.isSendEnabled -> {
                IconButton(
                    onClick = { onSendClick(state.inputText) },
                    enabled = state.selectedModel != null,

                    ) {
                    Icon(
                        painter = painterResource(Res.drawable.send),
                        contentDescription = "Send Message",
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModelState: ModelDetailState,
    modelsState: ModelsState,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onModelSelected: (ModelInfo) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {

    val modelsLoaded = modelsState is ModelsState.Success

    // Extract selected model name safely
    val selectedModelName = when (selectedModelState) {
        is ModelDetailState.Success -> {
            val family = selectedModelState.data.details?.family ?: "Unknown Family"
            val name = family + "-" + (selectedModelState.data.details?.parameterSize ?: "")
            name.uppercase()
        }


        ModelDetailState.Loading ->
            "Loading model..."

        is ModelDetailState.Error ->
            "Model error"

        ModelDetailState.Idle ->
            "Select model"
    }

    val buttonText = when (modelsState) {
        is ModelsState.Success -> selectedModelName
        ModelsState.Loading -> "Loading models..."
        is ModelsState.Error -> "Failed to load"
    }

    ExposedDropdownMenuBox(
        expanded = expanded && modelsLoaded,
        onExpandedChange = {
            if (modelsLoaded) onExpandChange(!expanded)
        },
        modifier = modifier
    ) {

        TextButton(
            onClick = { if (modelsLoaded) onExpandChange(true) },
            enabled = modelsLoaded,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor(
                ExposedDropdownMenuAnchorType.PrimaryNotEditable
            )
        ) {
            Text(
                text = buttonText,
                color = if (modelsLoaded)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            when {
                modelsState is ModelsState.Loading ||
                        selectedModelState is ModelDetailState.Loading -> {

                    Spacer(Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }

                else -> {
                    Icon(
                        imageVector = vectorResource(Res.drawable.keyboard_arrow_down),
                        contentDescription = null
                    )
                }
            }
        }

        ExposedDropdownMenu(
            expanded = expanded && modelsLoaded,
            onDismissRequest = { onExpandChange(false) }
        ) {

            when (modelsState) {

                ModelsState.Loading -> {
                    DropdownMenuItem(
                        text = { Text("Loading models...") },
                        onClick = {},
                        enabled = false
                    )
                }

                is ModelsState.Error -> {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Failed to load models")
                                Text(
                                    modelsState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        onClick = onRetry
                    )
                }

                is ModelsState.Success -> {
                    val models = modelsState.data.models

                    if (models.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No models found") },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.name)

                                        model.details?.parameterSize?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onModelSelected(model)
                                    onExpandChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}