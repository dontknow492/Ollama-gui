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
import com.ghost.ollama.gui.ui.components.InputBarState
import com.ghost.ollama.gui.ui.viewmodel.ChatSideEffect
import com.ghost.ollama.gui.ui.viewmodel.ChatUiState
import com.ghost.ollama.gui.ui.viewmodel.ChatViewModel
import com.ghost.ollama.gui.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import ollama_kmp.ollama_sample.generated.resources.Res
import ollama_kmp.ollama_sample.generated.resources.face
import ollama_kmp.ollama_sample.generated.resources.file_export
import ollama_kmp.ollama_sample.generated.resources.more_vert
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
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onEvent = sessionViewModel::onEvent,
                sideEffects = sessionViewModel.sideEffects,
                onSessionClick = viewModel::setCurrentSession
            )
        } else {
            DesktopChatScreen(
                state = state,
                sessionState = sessionState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onEvent = sessionViewModel::onEvent,
                sideEffects = sessionViewModel.sideEffects,
                onSessionClick = viewModel::setCurrentSession
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
    modifier: Modifier = Modifier
) {
//    var messages = state.messages
    var inputBarState by remember { mutableStateOf(InputBarState()) }

    val messages = viewModel.messages.collectAsLazyPagingItems()


    LaunchedEffect(state.isGenerating) {
        inputBarState = inputBarState.copy(isGenerating = state.isGenerating, isSendEnabled = !state.isGenerating)
    }




    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }, topBar = {
        ChatTopBar(
            appTitle = "Ollama",
            chatTitle = state.title,
            onExportClick = {},
            onMoreClick = {},
            onProfileClick = {})
    }) { paddingValues ->

        when (messages.itemCount == 0) {
            true -> EmptySessionScreen(
                modifier = Modifier.padding(paddingValues),
                userName = "Ollama",
                inputBarState = inputBarState,
                onInputChanged = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onSendClick = {
                    viewModel.sendMessage(it)
                    inputBarState = inputBarState.copy(inputText = "")
                },
                onSuggestionClick = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onStopClick = {})

            false -> ChatContentScreen(
                modifier = Modifier.padding(paddingValues),
                messages = messages,
                inputBarState = inputBarState,
                isMobile = isMobile,
                onMenuClick = onMenuClick,
                onInputChange = {
                    inputBarState = inputBarState.copy(inputText = it, isSendEnabled = it.isNotBlank())
                },
                onSendClick = {
                    viewModel.sendMessage(it)
                    inputBarState = inputBarState.copy(inputText = "")
                },
                onCopyMessage = viewModel::copyMessage,
                onDeleteMessage = viewModel::deleteMessage,

                )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    appTitle: String, chatTitle: String, onExportClick: () -> Unit, onMoreClick: () -> Unit, onProfileClick: () -> Unit
) {
    TopAppBar(title = {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {

            // Center Title (Chat Title)
            Text(
                text = chatTitle.ifBlank { "New Chat" },
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
        IconButton(onClick = onExportClick) {
            Icon(
                painter = painterResource(Res.drawable.file_export), contentDescription = "Export"
            )
        }

        // More Button
        IconButton(onClick = onMoreClick) {
            Icon(
                painter = painterResource(Res.drawable.more_vert), contentDescription = "More"
            )
        }

        // User Avatar
        IconButton(onClick = onProfileClick) {
            Surface(
                shape = CircleShape, tonalElevation = 2.dp, modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.face),
                        contentDescription = "Profile",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    })
}







