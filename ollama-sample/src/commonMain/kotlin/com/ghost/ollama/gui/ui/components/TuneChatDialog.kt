package com.ghost.ollama.gui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuneChatDialog(
    initialOptions: TuneOptions,
    onDismiss: () -> Unit,
    onApply: (TuneOptions) -> Unit
) {
    var options by remember { mutableStateOf(initialOptions) }
    var expandedSection by remember { mutableStateOf<Section?>(null) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            vectorResource(Res.drawable.tune),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tune Chat Parameters",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            vectorResource(Res.drawable.close),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Adjust the model parameters for this conversation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Parameters Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Temperature
                    ParameterSlider(
                        title = "Temperature",
                        description = "Controls randomness (0 = deterministic, 2 = very random)",
                        value = options.temperature ?: 0.7,
                        onValueChange = { options = options.copy(temperature = it) },
                        valueRange = 0.0..2.0,
                        steps = 19,
                        formatValue = { String.format("%.2f", it) },
                        icon = vectorResource(Res.drawable.whatshot),
                        expanded = expandedSection == Section.TEMPERATURE,
                        onExpandToggle = {
                            expandedSection = if (expandedSection == Section.TEMPERATURE) null else Section.TEMPERATURE
                        }
                    )

                    // Top K
                    ParameterSlider(
                        title = "Top K",
                        description = "Limits to top K most likely tokens",
                        value = options.topK?.toDouble() ?: 40.0,
                        onValueChange = { options = options.copy(topK = it.roundToLong()) },
                        valueRange = 1.0..100.0,
                        steps = 98,
                        formatValue = { it.roundToInt().toString() },
                        icon = vectorResource(Res.drawable.filter_alt),
                        expanded = expandedSection == Section.TOP_K,
                        onExpandToggle = {
                            expandedSection = if (expandedSection == Section.TOP_K) null else Section.TOP_K
                        }
                    )

                    // Top P
                    ParameterSlider(
                        title = "Top P",
                        description = "Nucleus sampling (0.1 = conservative, 1.0 = diverse)",
                        value = options.topP ?: 0.9,
                        onValueChange = { options = options.copy(topP = it) },
                        valueRange = 0.0..1.0,
                        steps = 19,
                        formatValue = { String.format("%.2f", it) },
                        icon = vectorResource(Res.drawable.trending_up),
                        expanded = expandedSection == Section.TOP_P,
                        onExpandToggle = {
                            expandedSection = if (expandedSection == Section.TOP_P) null else Section.TOP_P
                        }
                    )

                    // Min P
                    ParameterSlider(
                        title = "Min P",
                        description = "Minimum probability threshold",
                        value = options.minP ?: 0.0,
                        onValueChange = { options = options.copy(minP = it) },
                        valueRange = 0.0..1.0,
                        steps = 19,
                        formatValue = { String.format("%.2f", it) },
                        icon = vectorResource(Res.drawable.trending_down),
                        expanded = expandedSection == Section.MIN_P,
                        onExpandToggle = {
                            expandedSection = if (expandedSection == Section.MIN_P) null else Section.MIN_P
                        }
                    )

                    // Seed
                    ParameterTextField(
                        title = "Seed",
                        description = "Random seed for reproducibility",
                        value = options.seed?.toString() ?: "",
                        onValueChange = {
                            options = options.copy(seed = it.toLongOrNull())
                        },
                        placeholder = "Random",
                        icon = vectorResource(Res.drawable.style),
                        keyboardType = KeyboardType.Number
                    )

                    // Stop Sequences
                    ParameterTextField(
                        title = "Stop Sequences",
                        description = "Stop generation at these sequences",
                        value = options.stop ?: "",
                        onValueChange = { options = options.copy(stop = it) },
                        placeholder = "e.g., \\n, User:",
                        icon = vectorResource(Res.drawable.stop_circle),
                        supportingText = "Comma separated"
                    )

                    // Context Size
                    ParameterSlider(
                        title = "Context Size",
                        description = "Maximum context window",
                        value = options.numCtx?.toDouble() ?: 2048.0,
                        onValueChange = { options = options.copy(numCtx = it.roundToLong()) },
                        valueRange = 512.0..8192.0,
                        steps = 15,
                        formatValue = { it.roundToInt().toString() },
                        icon = vectorResource(Res.drawable.quickreply),
                        expanded = expandedSection == Section.NUM_CTX,
                        onExpandToggle = {
                            expandedSection = if (expandedSection == Section.NUM_CTX) null else Section.NUM_CTX
                        }
                    )

                    // Max Predict
                    ParameterSlider(
                        title = "Max Tokens",
                        description = "Maximum tokens to generate",
                        value = options.numPredict?.toDouble() ?: 2048.0,
                        onValueChange = { options = options.copy(numPredict = it.roundToLong()) },
                        valueRange = 64.0..4096.0,
                        steps = 63,
                        formatValue = { it.roundToInt().toString() },
                        icon = vectorResource(Res.drawable.numbers),
                        expanded = expandedSection == Section.NUM_PREDICT,
                        onExpandToggle = {
                            expandedSection = if (expandedSection == Section.NUM_PREDICT) null else Section.NUM_PREDICT
                        }
                    )

                    // Format
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    vectorResource(Res.drawable.side_navigation),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Response Format",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                listOf("text", "json").forEach { format ->
                                    FilterChip(
                                        selected = options.format == format,
                                        onClick = {
                                            options = options.copy(
                                                format = if (options.format == format) null else format
                                            )
                                        },
                                        label = {
                                            Text(
                                                format.uppercase(),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { options = TuneOptions() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onApply(options)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            vectorResource(Res.drawable.api),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    title: String,
    description: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    valueRange: ClosedFloatingPointRange<Double>,
    steps: Int,
    formatValue: (Double) -> String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(
                    onClick = onExpandToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) vectorResource(Res.drawable.keyboard_arrow_down)
                        else vectorResource(Res.drawable.trending_up),
                        contentDescription = if (expanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatValue(value),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(60.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChange(it.toDouble()) },
                    valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
                    steps = steps,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

@Composable
private fun ParameterTextField(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )

            if (supportingText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Data class for tune options
data class TuneOptions(
    val seed: Long? = null,
    val temperature: Double? = null,
    val topK: Long? = null,
    val topP: Double? = null,
    val minP: Double? = null,
    val stop: String? = null,
    val numCtx: Long? = null,
    val numPredict: Long? = null,
    val format: String? = null
)

private enum class Section {
    TEMPERATURE, TOP_K, TOP_P, MIN_P, NUM_CTX, NUM_PREDICT
}

// Extension to merge with ChatOptions
fun TuneOptions.mergeWithChatOptions(): TuneOptions {
    return this // Implement merging logic if needed
}