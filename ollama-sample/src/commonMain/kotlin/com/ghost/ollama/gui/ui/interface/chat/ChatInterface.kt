package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.compose.collectAsLazyPagingItems
import com.ghost.ollama.gui.SessionView
import com.ghost.ollama.gui.models.ModelDetailState
import com.ghost.ollama.gui.models.ModelsState
import com.ghost.ollama.gui.ui.components.*
import com.ghost.ollama.gui.ui.`interface`.RenameSessionDialog
import com.ghost.ollama.gui.utils.toTuneOptions
import com.ghost.ollama.gui.viewmodel.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import ollama_kmp.ollama_sample.generated.resources.Res
import ollama_kmp.ollama_sample.generated.resources.clear_all
import ollama_kmp.ollama_sample.generated.resources.left_panel_open
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
    sessionViewModel: SessionViewModel = koinViewModel()
) {
    // 1. Observe the UI State
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()

    // Multiplatform contexts for side effects
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Sidebar States
    rememberDrawerState(initialValue = DrawerValue.Closed)

    // 2. Observe Side Effects (One-off events)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatSideEffect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                    scope.launch {
                        snackbarHostState.showSnackbar("Copied to clipboard")
                    }
                }

                is ChatSideEffect.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    }

    // 3. Responsive Layout Strategy
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp

        if (isMobile) {
            MobileChatScreen(
                state = state,
                sessionState = sessionState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onEvent = sessionViewModel::onEvent,
                sideEffects = sessionViewModel.sideEffects,
                onChatEvent = viewModel::onEvent
            )
        } else {
            DesktopChatScreen(
                state = state,
                sessionState = sessionState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onEvent = sessionViewModel::onEvent,
                sideEffects = sessionViewModel.sideEffects,
                onChatEvent = viewModel::onEvent
            )
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMainContent(
    state: ChatUiState,
    viewModel: ChatViewModel,
    snackbarHostState: SnackbarHostState,
    isMobile: Boolean,
    onMenuClick: () -> Unit,
    onChatEvent: (ChatEvent) -> Unit,
    onSessionEvent: (SessionEvent) -> Unit,
    modifier: Modifier = Modifier
) {

    val chatState by viewModel.state.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModelDetailed.collectAsStateWithLifecycle()
    val installedModels by viewModel.installedModels.collectAsStateWithLifecycle(ModelsState.Loading)

    var renameSession by remember { mutableStateOf<SessionView?>(null) }
    var deleteSession by remember { mutableStateOf<SessionView?>(null) }
//    var messages = state.messages
    var inputBarState by remember(installedModels, selectedModel) {
        Napier.d { "Recomputing inputBarState with selectedModel: $selectedModel and installedModels: $installedModels" }
        mutableStateOf(
            InputBarState(
                selectedModel = selectedModel ?: ModelDetailState.Idle,
                installedModels = installedModels
            )
        )
    }
    var isTuneVisible by remember { mutableStateOf(false) }


    val tuneOptions = remember(chatState.session) {
        chatState.session?.toTuneOptions()
    }

    val messages = viewModel.messages.collectAsLazyPagingItems()


    LaunchedEffect(state.isGenerating) {
        inputBarState = inputBarState.copy(isGenerating = state.isGenerating, isSendEnabled = !state.isGenerating)
    }




    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                appTitle = "Ollama",
                session = state.session,
                onClearChat = { onChatEvent(ChatEvent.ClearChat) },
                onRename = { renameSession = it },
                onDeleteSession = { deleteSession = it },
                onEvent = onSessionEvent
            )
        },
        floatingActionButton = {
            if (isMobile) {
                FloatingActionButton(
                    onClick = onMenuClick,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.left_panel_open),
                        contentDescription = "Menu"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { paddingValues ->

        when (messages.itemCount == 0) {
            true -> EmptySessionScreen(
                modifier = Modifier
                    .padding(paddingValues),
                userName = "Ollama",
                inputBarState = inputBarState,
                onInputChanged = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onChatEvent = onChatEvent,
                onSuggestionClick = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onToolsClick = { isTuneVisible = true },
                onModelSelected = { onChatEvent(ChatEvent.SelectModel(it)) },
                onRetryModel = { /* Handle retry model if needed */ },
            )

            false -> ChatContentScreen(
                modifier = Modifier
//                    .padding(paddingValues)
                ,
                messages = messages,
                inputBarState = inputBarState,
                isMobile = isMobile,
                onInputChange = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onChatEvent = onChatEvent,
                onToolsClicked = { isTuneVisible = true }
            )
        }
    }

    if (renameSession != null) {
        RenameSessionDialog(
            session = renameSession!!,
            onDismiss = { renameSession = null },
            onConfirm = { newTitle ->
                onSessionEvent(SessionEvent.RenameSession(renameSession!!.id, newTitle))
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
                onSessionEvent(SessionEvent.DeleteSession(deleteSession!!.id))
                deleteSession = null
            },
            onDismiss = { deleteSession = null }
        )
    }

    if (isTuneVisible && tuneOptions != null) {
        TuneChatDialog(
            initialOptions = tuneOptions,
            onDismiss = { isTuneVisible = false },
            onApply = {
                onSessionEvent(
                    SessionEvent.UpdateSessionTuneOptions(it, session = state.session!!)
                )
            }
        )
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    appTitle: String,
    session: SessionView?,
    onClearChat: (SessionView) -> Unit,
    onRename: (SessionView) -> Unit,
    onDeleteSession: (SessionView) -> Unit,
    onEvent: (SessionEvent) -> Unit
) {
    var isMenuVisible by remember { mutableStateOf(false) }
    TopAppBar(title = {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            // Center Title (Chat Title)
            Text(
                text = session?.title ?: "New Chat",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )

            // Left App Title
            Text(
                text = appTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }, actions = {

        // Export Button
        PinButton(
            pinned = session?.pinned ?: false,
            onTogglePin = {
                if (session == null) return@PinButton
                onEvent(SessionEvent.ToggleSessionPin(session.id))
            }
        )

        // More Button
        if (session != null) {
            SessionMenu(
                session = session,
                isSelectionModeActive = false,
                onRename = {
                    isMenuVisible = false
                    onRename(session)
                },
                onDeleteSession = {
                    isMenuVisible = false
                    onDeleteSession(session)
                },
                onEvent = onEvent
            )
        }

        // User Avatar
        if (session != null) {
            IconButton(onClick = { onClearChat(session) }) {
                Surface(
                    shape = CircleShape, tonalElevation = 2.dp, modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.clear_all),
                            contentDescription = "Clear Chat",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

    })
}







