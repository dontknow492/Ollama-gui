package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghost.ollama.gui.ui.`interface`.SideBar
import com.ghost.ollama.gui.ui.viewmodel.ChatUiState
import com.ghost.ollama.gui.ui.viewmodel.SessionUiState
import kotlinx.coroutines.launch


@Composable
fun MobileChatScreen(
    state: ChatUiState,
    sessionState: SessionUiState,
    snackbarHostState: SnackbarHostState,
    onSendMessage: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier
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
                    onToggle = { scope.launch { mobileDrawerState.close() } }
                )
            }
        }
    ) {
        ChatMainContent(
            state = state,
            snackbarHostState = snackbarHostState,
            isMobile = true,
            onMenuClick = { scope.launch { mobileDrawerState.open() } },
            onSendMessage = onSendMessage,
            onCopyMessage = onCopyMessage,
            onDeleteMessage = onDeleteMessage
        )
    }
}