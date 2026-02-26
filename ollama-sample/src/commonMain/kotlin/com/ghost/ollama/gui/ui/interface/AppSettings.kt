package com.ghost.ollama.gui.ui.`interface`

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.ollama.gui.repository.AppTheme
import com.ghost.ollama.gui.viewmodel.GlobalSettingsEditState
import com.ghost.ollama.gui.viewmodel.GlobalSettingsViewModel
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: GlobalSettingsViewModel = koinViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val errors by viewModel.validationErrors.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Model", "Advanced")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .widthIn(max = 500.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(vectorResource(Res.drawable.close), contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTabIndex) {
                        0 -> GeneralSettingsTab(editState, errors, viewModel)
                        1 -> ModelSettingsTab(editState, errors, viewModel)
                        2 -> AdvancedSettingsTab(editState, errors, viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Success message
                    if (uiState.showSuccessMessage) {
                        AssistChip(
                            onClick = { viewModel.dismissSuccessMessage() },
                            label = { Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = "Settings saved") },
                            leadingIcon = {
                                Icon(
                                    vectorResource(Res.drawable.api),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            viewModel.resetToDefaults()
                            selectedTabIndex = 0
                        }
                    ) {
                        Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = "Reset to defaults")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = viewModel::saveSettings,
                        enabled = errors.isEmpty()
                    ) {
                        Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    state: GlobalSettingsEditState,
    errors: Map<String, String>,
    viewModel: GlobalSettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Base URL
        InputField(
            label = "Base URL",
            value = state.baseUrl,
            onValueChange = viewModel::updateBaseUrl,
            error = errors["baseUrl"],
            leadingIcon = vectorResource(Res.drawable.link),
            keyboardType = KeyboardType.Uri
        )

        // Default Model
        InputField(
            label = "Default Model",
            value = state.defaultModel,
            onValueChange = viewModel::updateDefaultModel,
            error = errors["defaultModel"],
            leadingIcon = vectorResource(Res.drawable.smart_toy)
        )

        // Theme Selection
        Text(
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = "Theme",
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppTheme.values().forEach { theme ->
                FilterChip(
                    selected = state.theme == theme,
                    onClick = { viewModel.updateTheme(theme) },
                    label = {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = viewModel.getThemeDisplayName(theme)
                        )
                    }
                )
            }
        }

        // Max History Length
        val historyLength = state.maxHistoryLength
        Text(
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            text = "Max History Length: $historyLength",
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = historyLength.toFloat(),
            onValueChange = { viewModel.updateMaxHistoryLength(it.roundToInt()) },
            valueRange = 10f..500f,
            steps = 48,
            modifier = Modifier.fillMaxWidth()
        )
        if (errors.containsKey("maxHistoryLength")) {
            Text(
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                text = errors["maxHistoryLength"]!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Auto Save
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = "Auto Save",
                style = MaterialTheme.typography.titleSmall
            )
            Switch(
                checked = state.autoSave,
                onCheckedChange = viewModel::updateAutoSave
            )
        }
    }
}

@Composable
private fun ModelSettingsTab(
    state: GlobalSettingsEditState,
    errors: Map<String, String>,
    viewModel: GlobalSettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Temperature
        Text(
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            text = "Temperature: ${viewModel.formatTemperatureForDisplay(state.temperature)}",
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = state.temperature,
            onValueChange = viewModel::updateTemperature,
            valueRange = 0.0f..2.0f,
            steps = 19,
            modifier = Modifier.fillMaxWidth()
        )
        if (errors.containsKey("temperature")) {
            Text(
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                text = errors["temperature"]!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Seed
        InputField(
            label = "Seed (optional)",
            value = state.seed?.toString() ?: "",
            onValueChange = { newValue ->
                viewModel.updateSeed(newValue.toIntOrNull())
            },
            error = errors["seed"],
            leadingIcon = vectorResource(Res.drawable.style),
            keyboardType = KeyboardType.Number
        )

        // Top K
        InputField(
            label = "Top K (optional)",
            value = state.topK?.toString() ?: "",
            onValueChange = { newValue ->
                viewModel.updateTopK(newValue.toIntOrNull())
            },
            error = errors["topK"],
            leadingIcon = vectorResource(Res.drawable.filter_alt),
            keyboardType = KeyboardType.Number
        )

        // Top P
        InputField(
            label = "Top P (optional, 0-1)",
            value = state.topP?.toString() ?: "",
            onValueChange = { newValue ->
                viewModel.updateTopP(newValue.toFloatOrNull())
            },
            error = errors["topP"],
            leadingIcon = vectorResource(Res.drawable.trending_up),
            keyboardType = KeyboardType.Decimal
        )

        // Min P
        InputField(
            label = "Min P (optional, 0-1)",
            value = state.minP?.toString() ?: "",
            onValueChange = { newValue ->
                viewModel.updateMinP(newValue.toFloatOrNull())
            },
            error = errors["minP"],
            leadingIcon = vectorResource(Res.drawable.trending_down),
            keyboardType = KeyboardType.Decimal
        )
    }
}

@Composable
private fun AdvancedSettingsTab(
    state: GlobalSettingsEditState,
    errors: Map<String, String>,
    viewModel: GlobalSettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stop sequences
        InputField(
            label = "Stop sequences (comma separated)",
            value = state.stop,
            onValueChange = viewModel::updateStop,
            leadingIcon = vectorResource(Res.drawable.stop_circle),
            supportingText = "e.g., \\n, User:, Assistant:"
        )

        // Num Ctx
        InputField(
            label = "Context size (optional)",
            value = state.numCtx?.toString() ?: "",
            onValueChange = { newValue ->
                viewModel.updateNumCtx(newValue.toIntOrNull())
            },
            error = errors["numCtx"],
            leadingIcon = vectorResource(Res.drawable.quickreply),
            keyboardType = KeyboardType.Number
        )

        // Num Predict
        InputField(
            label = "Max tokens (optional)",
            value = state.numPredict?.toString() ?: "",
            onValueChange = { newValue ->
                viewModel.updateNumPredict(newValue.toIntOrNull())
            },
            error = errors["numPredict"],
            leadingIcon = vectorResource(Res.drawable.numbers),
            keyboardType = KeyboardType.Number
        )

        // Response Format
        Text(
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = "Response Format",
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilterChip(
                selected = !viewModel.isFormatJson(),
                onClick = viewModel::setFormatText,
                label = { Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = "Text") }
            )
            FilterChip(
                selected = viewModel.isFormatJson(),
                onClick = viewModel::setFormatJson,
                label = { Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = "JSON") }
            )
        }
    }
}

@Composable
private fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null) }
        },
        supportingText = {
            if (error != null) {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (supportingText != null) {
                Text(maxLines = 1, overflow = TextOverflow.Ellipsis, text = supportingText)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        maxLines = 1,
    )
}