package com.ghost.ollama.gui.ui.`interface`

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import app.cash.paging.compose.LazyPagingItems
import app.cash.paging.compose.collectAsLazyPagingItems
import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.models.rememberPlatformConfiguration
import com.ghost.ollama.gui.ui.components.ChatScrollbar
import com.ghost.ollama.gui.ui.components.ConfirmationDialog
import com.ghost.ollama.gui.ui.components.PinButton
import com.ghost.ollama.gui.ui.components.SessionMenu
import com.ghost.ollama.gui.viewmodel.SessionEvent
import com.ghost.ollama.gui.viewmodel.SessionSideEffect
import com.ghost.ollama.gui.viewmodel.SessionUiState
import kotlinx.coroutines.flow.Flow
import ollama_kmp.ollama_sample.generated.resources.*
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideBar(
    modifier: Modifier = Modifier,
    state: SessionUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onSessionClick: (String) -> Unit,
    onEvent: (SessionEvent) -> Unit,
    sideEffects: Flow<SessionSideEffect>
) {

    val configuration = rememberPlatformConfiguration()
    configuration.screenWidthDp >= 600 // Typical tablet threshold
    LocalDensity.current
    val expandedWidth = 320.dp
    val collapsedWidth = 80.dp
    val animatedWidth by animateDpAsState(
        targetValue = if (expanded) expandedWidth else collapsedWidth,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "sidebarWidth"
    )

    var renameSession by remember { mutableStateOf<SessionView?>(null) }
    var deleteSession by remember { mutableStateOf<SessionView?>(null) }

    val ollamaVersion = rememberSaveable(state) {
        if (state is SessionUiState.Success) state.ollamaVersion else "unknown"
    }
    val isOnline = rememberSaveable(state) {
        if (state is SessionUiState.Success) state.isOllamaRunning else false
    }

//     Handle side effects
    LaunchedEffect(Unit) {
        sideEffects.collect { effect ->
            when (effect) {
                is SessionSideEffect.ShowToast -> {
                    // Show toast using your preferred method (e.g., SnackbarHostState)
                    // You can pass a SnackbarHostState from the parent
                }

                is SessionSideEffect.ExportFileReady -> {
                    // Trigger file export (platform-specific)
                }

                is SessionSideEffect.NewSessionCreated -> {
                    // Optionally navigate to the new session
                    onSessionClick(effect.sessionId)
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
        tonalElevation = 1.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (expanded) 8.dp else 4.dp)
        ) {
            Header(
                isExpanded = expanded,
                onToggle = onToggle
            )

            when (state) {
                is SessionUiState.Error -> {
                    SessionErrorState(
                        message = state.message,
                        isExpanded = expanded,
                        onRetry = { onEvent(SessionEvent.Retry) },
                        modifier = Modifier.padding(8.dp).weight(1f)
                    )
                }

                SessionUiState.Loading -> {
                    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is SessionUiState.Success -> {
                    val pagingItems = state.pagedSessions.collectAsLazyPagingItems()

                    // New chat button
                    NewChatButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        isExpanded = expanded,
                        onClick = { onEvent(SessionEvent.CreateNew(null)) }
                    )

                    // Search bar (only when expanded)
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { onEvent(SessionEvent.Search(it)) },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Selection mode toolbar (appears when items selected)
                    AnimatedVisibility(
                        visible = state.isSelectionModeActive,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 })
                    ) {
                        SelectionToolbar(
                            selectedCount = state.selectedSessionIds.size,
                            isExporting = state.isExporting,
                            onDelete = { onEvent(SessionEvent.BatchDelete) },
                            onExport = { onEvent(SessionEvent.BatchExport) },
                            onClear = { onEvent(SessionEvent.ClearSelection) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    SessionList(
                        isExpanded = expanded,
                        pagingItems = pagingItems,
                        selectedIds = state.selectedSessionIds,
                        isSelectionModeActive = state.isSelectionModeActive,
                        onEvent = onEvent,
                        onRename = { renameSession = it },
                        onDeleteSession = { deleteSession = it },
                        onSessionClick = { onSessionClick(it.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sessions list


            // Bottom section: version and settings
            BottomSection(
                isOnline = isOnline,
                isExpanded = expanded,
                ollamaVersion = ollamaVersion,
                onSettingsClick = onSettingsClick
            )
        }

    }




    if (renameSession != null) {
        RenameSessionDialog(
            session = renameSession!!,
            onDismiss = { renameSession = null },
            onConfirm = { newTitle ->
                onEvent(SessionEvent.RenameSession(renameSession!!.id, newTitle))
                renameSession = null
            }
        )
    }

    if (deleteSession != null) {
        ConfirmationDialog(
            title = "Delete Session",
            message = "Are you sure you want to delete the session \"${deleteSession!!.title}\"? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onEvent(SessionEvent.DeleteSession(deleteSession!!.id))
                deleteSession = null
            },
            onDismiss = { deleteSession = null }
        )
    }
}

@Composable
fun SessionErrorState(
    message: String,
    isExpanded: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isExpanded) {
            ExpandedErrorContent(
                message = message,
                onRetry = onRetry
            )
        } else {
            CollapsedErrorContent(
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun ExpandedErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = vectorResource(Res.drawable.error),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry")
            }
        }
    }
}


@Composable
private fun CollapsedErrorContent(
    onRetry: () -> Unit
) {
    IconButton(
        onClick = onRetry,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.error),
            contentDescription = "Retry",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun Header(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        if (isExpanded) {
            Text(
                text = "Ollama",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = vectorResource(
                    if (isExpanded) Res.drawable.left_panel_close // Your icon name
                    else Res.drawable.left_panel_open // Your icon name
                ),
                contentDescription = if (isExpanded) "Collapse sidebar" else "Expand sidebar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NewChatButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                vectorResource(Res.drawable.new_window),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Chat", fontSize = 14.sp)
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .padding(8.dp)
                .size(48.dp)
        ) {
            Icon(
                vectorResource(Res.drawable.new_window),
                contentDescription = "New Chat",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search chats...") },
        leadingIcon = { Icon(vectorResource(Res.drawable.search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(vectorResource(Res.drawable.clear_all), contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun SelectionToolbar(
    selectedCount: Int,
    isExporting: Boolean,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onExport, enabled = !isExporting) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(vectorResource(Res.drawable.file_export), contentDescription = "Export selected")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(vectorResource(Res.drawable.delete_forever), contentDescription = "Delete selected")
            }
            IconButton(onClick = onClear) {
                Icon(vectorResource(Res.drawable.clear_all), contentDescription = "Clear selection")
            }
        }
    }
}

@Composable
fun SessionList(
    isExpanded: Boolean,
    pagingItems: LazyPagingItems<SessionView>,
    selectedIds: Set<String>,
    isSelectionModeActive: Boolean,
    onSessionClick: (SessionView) -> Unit,
    onEvent: (SessionEvent) -> Unit,
    onRename: (SessionView) -> Unit,
    onDeleteSession: (SessionView) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val pinnedItems = mutableListOf<SessionView>()
            val chatItems = mutableListOf<SessionView>()

            for (i in 0 until pagingItems.itemCount) {
                val item = pagingItems[i]
                if (item != null) {
                    if (item.pinned) pinnedItems.add(item)
                    else chatItems.add(item)
                }
            }

            if (pinnedItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Pinned", isExpanded = isExpanded)
                }

                items(
                    items = pinnedItems,
                    key = { it.id }
                ) { session ->
                    SessionItem(
                        isExpanded = isExpanded,
                        session = session,
                        isSelected = session.id in selectedIds,
                        isSelectionModeActive = isSelectionModeActive,
                        onEvent = onEvent,
                        onRename = onRename,
                        onDeleteSession = onDeleteSession,
                        onSessionClick = onSessionClick,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (chatItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Chats", isExpanded = isExpanded)
                }

                items(
                    items = chatItems,
                    key = { it.id }
                ) { session ->
                    SessionItem(
                        isExpanded = isExpanded,
                        session = session,
                        isSelected = session.id in selectedIds,
                        isSelectionModeActive = isSelectionModeActive,
                        onEvent = onEvent,
                        onRename = onRename,
                        onDeleteSession = onDeleteSession,
                        onSessionClick = onSessionClick,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Handle loading and error states within the list
            when (pagingItems.loadState.refresh) {
                is LoadState.Loading -> {
                    item { LoadingItem() }
                }

                is LoadState.Error -> {
                    val error = pagingItems.loadState.refresh as LoadState.Error
                    item { ErrorItem(message = error.error.message ?: "Unknown error") }
                }

                else -> {}
            }

            when (pagingItems.loadState.append) {
                is LoadState.Loading -> {
                    item { LoadingItem() }
                }

                is LoadState.Error -> {
                    val error = pagingItems.loadState.append as LoadState.Error
                    item { ErrorItem(message = error.error.message ?: "Unknown error") }
                }

                else -> {}
            }
        }

        ChatScrollbar(
            listState = listState,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    } else {
        HorizontalDivider(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionItem(
    isExpanded: Boolean,
    session: SessionView,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    onRename: (SessionView) -> Unit,
    onSessionClick: (SessionView) -> Unit,
    onDeleteSession: (SessionView) -> Unit,
    onEvent: (SessionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val avatarColor = remember(session.id) {
        generateColorFromId(session.id)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = {
                    if (isSelectionModeActive) {
                        onEvent(SessionEvent.ToggleSelection(session.id))
                    } else {
                        onSessionClick(session)
                    }
                },
                onLongClick = {
                    if (!isSelectionModeActive) {
                        onEvent(SessionEvent.ToggleSelection(session.id))
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.Transparent
        ),
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        if (isExpanded) {

            // ================= EXPANDED VIEW =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (isSelectionModeActive) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            onEvent(SessionEvent.ToggleSelection(session.id))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDate(session.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PinButton(
                    pinned = session.pinned,
                    onTogglePin = { onEvent(SessionEvent.ToggleSessionPin(session.id)) }
                )



                SessionMenu(session, isSelectionModeActive, onRename, onDeleteSession, onEvent)
            }

        } else {

            // ================= COLLAPSED VIEW =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = avatarColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = session.title.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun RenameSessionDialog(
    session: SessionView,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(session.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Session") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Session Title") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


fun generateColorFromId(id: String): Color {
    val colors = listOf(
        Color(0xFFEF5350),
        Color(0xFFAB47BC),
        Color(0xFF5C6BC0),
        Color(0xFF29B6F6),
        Color(0xFF26A69A),
        Color(0xFFFFA726),
        Color(0xFF8D6E63)
    )
    val index = kotlin.math.abs(id.hashCode()) % colors.size
    return colors[index]
}

@Composable
fun LoadingItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
fun ErrorItem(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }
}

@Composable
fun BottomSection(
    isOnline: Boolean,
    isExpanded: Boolean,
    ollamaVersion: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = rememberSaveable(isOnline) {
        if (isOnline) "Ollama Online" else "Ollama Offline"
    }
    val versionText = rememberSaveable(ollamaVersion) {
        if (isOnline) "v$ollamaVersion" else "Version Unknown"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            if (isExpanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated Status Dot
                    StatusIcon(isOnline = !ollamaVersion.equals("unknown", ignoreCase = true))

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = versionText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.settings),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusIcon(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.smart_toy),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )

        // Status Dot (bottom-end corner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    if (isOnline)
                        Color(0xFF4CAF50)   // Green
                    else
                        Color(0xFFF44336)   // Red
                )
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )
    }
}

// Helper function to format date (placeholder)
fun formatDate(timestamp: Long): String {
    // Use platform-specific date formatting
    return "Just now"
}
