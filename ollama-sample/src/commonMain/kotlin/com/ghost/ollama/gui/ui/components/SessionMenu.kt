package com.ghost.ollama.gui.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.viewmodel.SessionEvent
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SessionMenu(
    session: SessionView,
    isSelectionModeActive: Boolean,
    onRename: (SessionView) -> Unit,
    onDeleteSession: (SessionView) -> Unit,
    onEvent: (SessionEvent) -> Unit
) {
    if (isSelectionModeActive) return

    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = vectorResource(Res.drawable.more_vert),
                contentDescription = "Session options"
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    menuExpanded = false
                    onRename(session)
                },
                leadingIcon = {
                    Icon(vectorResource(Res.drawable.edit), null)
                }
            )

            DropdownMenuItem(
                text = { Text("Export") },
                onClick = {
                    menuExpanded = false
                    onEvent(SessionEvent.ExportSession(session.id))
                },
                leadingIcon = {
                    Icon(vectorResource(Res.drawable.file_export), null)
                }
            )

            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    menuExpanded = false
                    onDeleteSession(session)
                },
                leadingIcon = {
                    Icon(vectorResource(Res.drawable.delete_forever), null)
                }
            )
        }
    }
}


@Composable
fun PinButton(
    pinned: Boolean,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onTogglePin,
        modifier = modifier,
    ) {
        Icon(
            vectorResource(
                if (pinned) Res.drawable.keep_off else Res.drawable.keep
            ),
            contentDescription = "toggle pin",
            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}