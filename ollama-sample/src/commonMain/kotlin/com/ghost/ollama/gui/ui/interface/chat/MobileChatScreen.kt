package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghost.ollama.gui.ui.`interface`.SideBar
import com.ghost.ollama.gui.ui.viewmodel.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch


@Composable
fun MobileChatScreen(
    modifier: Modifier = Modifier,
    state: ChatUiState,
    viewModel: ChatViewModel,
    sessionState: SessionUiState,
    snackbarHostState: SnackbarHostState,
    onSessionClick: (String) -> Unit,
    onEvent: (SessionEvent) -> Unit,
    sideEffects: Flow<SessionSideEffect>

) {
    val mobileDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // --- MOBILE LAYOUT: Hidden drawer + Floating Menu Button ---
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = mobileDrawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                SideBar(
                    state = sessionState,
                    expanded = true,
                    onToggle = { scope.launch { mobileDrawerState.close() } },
                    onEvent = onEvent,
                    sideEffects = sideEffects,
                    onSessionClick = {
                        onSessionClick(it)
                        scope.launch { mobileDrawerState.close() }
                    }
                )
            }
        }
    ) {
        ChatMainContent(
            state = state,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            isMobile = true,
            onMenuClick = { scope.launch { mobileDrawerState.open() } }
        )
    }
}