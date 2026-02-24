package com.ghost.ollama.gui.ui.`interface`.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghost.ollama.gui.ui.`interface`.SideBar
import com.ghost.ollama.gui.ui.viewmodel.*
import kotlinx.coroutines.flow.Flow

@Composable
fun DesktopChatScreen(
    modifier: Modifier = Modifier,
    state: ChatUiState,
    viewModel: ChatViewModel,
    sessionState: SessionUiState,
    snackbarHostState: SnackbarHostState,
    onSessionClick: (String) -> Unit,
    onEvent: (SessionEvent) -> Unit,
    sideEffects: Flow<SessionSideEffect>

) {
    var isDesktopSidebarExpanded by remember { mutableStateOf(false) }

    // --- DESKTOP/TABLET LAYOUT: Persistent Side Rail / Drawer ---
    Row(modifier = modifier.fillMaxSize()) {
        if (isDesktopSidebarExpanded) {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(0.dp) // Flush with screen edge
            ) {
                SideBar(
                    modifier = Modifier.width(320.dp),
                    state = sessionState,
                    expanded = true,
                    onToggle = { isDesktopSidebarExpanded = false },
                    onEvent = onEvent,
                    sideEffects = sideEffects,
                    onSessionClick = onSessionClick
                )
            }
        } else {
            NavigationRail(
                containerColor = DrawerDefaults.modalContainerColor
            ) {
                // FIX: Ensure expanded is false for the rail so the UI knows it's collapsed
                SideBar(
                    state = sessionState,
                    expanded = false,
                    onToggle = { isDesktopSidebarExpanded = true },
                    onEvent = onEvent,
                    sideEffects = sideEffects,
                    onSessionClick = onSessionClick
                )
            }
        }

        ChatMainContent(
            state = state,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            isMobile = false,
            onMenuClick = { }, // Unused on desktop
            modifier = Modifier.weight(1f)
        )
    }
}