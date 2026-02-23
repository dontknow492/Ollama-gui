package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghost.ollama.gui.ui.components.InputBarState
import com.ghost.ollama.gui.ui.viewmodel.ChatSideEffect
import com.ghost.ollama.gui.ui.viewmodel.ChatUiState
import com.ghost.ollama.gui.ui.viewmodel.ChatViewModel
import com.ghost.ollama.gui.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
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

    //

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
                snackbarHostState = snackbarHostState,
                onSendMessage = viewModel::sendMessage,
                onCopyMessage = viewModel::copyMessage,
                onDeleteMessage = viewModel::deleteMessage
            )
        } else {
            DesktopChatScreen(
                state = state,
                sessionState = sessionState,
                snackbarHostState = snackbarHostState,
                onSendMessage = viewModel::sendMessage,
                onCopyMessage = viewModel::copyMessage,
                onDeleteMessage = viewModel::deleteMessage
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMainContent(
    state: ChatUiState,
    snackbarHostState: SnackbarHostState,
    isMobile: Boolean,
    onMenuClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    var inputBarState by remember { mutableStateOf(InputBarState()) }

    LaunchedEffect(state.isGenerating) {
        inputBarState = inputBarState.copy(isGenerating = state.isGenerating, isSendEnabled = !state.isGenerating)
    }




    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(state.messages.firstOrNull()?.content ?: "")
                }
            )
        }
    ) { paddingValues ->

        when (state.messages.isEmpty()) {
            true -> EmptySessionScreen(
                modifier = Modifier.padding(paddingValues),
                userName = "Ollama",
                inputBarState = inputBarState,
                onInputChanged = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onSendClick = {
                    onSendMessage(it)
                    inputBarState = inputBarState.copy(inputText = "")
                },
                onSuggestionClick = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onStopClick = {}
            )

            false -> ChatContentScreen(
                modifier = Modifier.padding(paddingValues),
                messages = state.messages,
                inputBarState = inputBarState,
                isMobile = isMobile,
                onMenuClick = onMenuClick,
                onInputChange = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onSendClick = {
                    onSendMessage(it)
                    inputBarState = inputBarState.copy(inputText = "")
                },
                onCopyMessage = onCopyMessage,
                onDeleteMessage = onDeleteMessage,

                )
        }
    }
}








