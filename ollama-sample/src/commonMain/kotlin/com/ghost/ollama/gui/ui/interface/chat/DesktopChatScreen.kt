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
import com.ghost.ollama.gui.ui.`interface`.SettingsDialog
import com.ghost.ollama.gui.ui.`interface`.SideBar
import com.ghost.ollama.gui.viewmodel.*
import kotlinx.coroutines.flow.Flow

@Composable
fun DesktopChatScreen(
    modifier: Modifier = Modifier,
    state: ChatUiState,
    viewModel: ChatViewModel,
    sessionState: SessionUiState,
    snackbarHostState: SnackbarHostState,
    onChatEvent: (ChatEvent) -> Unit,
    onEvent: (SessionEvent) -> Unit,
    onDownloadButtonClick: () -> Unit,
    sideEffects: Flow<SessionSideEffect>

) {
    var isDesktopSidebarExpanded by remember { mutableStateOf(false) }
    var isSettingOpen by remember { mutableStateOf(false) }

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
                    onSettingsClick = { isSettingOpen = true },
                    onEvent = onEvent,
                    sideEffects = sideEffects,
                    onSessionClick = { onChatEvent(ChatEvent.SessionSelected(it)) }
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
                    onSettingsClick = { isSettingOpen = true },
                    onEvent = onEvent,
                    sideEffects = sideEffects,
                    onSessionClick = { onChatEvent(ChatEvent.SessionSelected(it)) }
                )
            }
        }

        ChatMainContent(
            state = state,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            isMobile = false,
            onMenuClick = { }, // Unused on desktop
            onChatEvent = onChatEvent,
            onSessionEvent = onEvent,
            onDownloadButtonClick = onDownloadButtonClick,
            modifier = Modifier.weight(1f)
        )
    }

    if (isSettingOpen) {
        // Show settings dialog or screen
        SettingsDialog {
            isSettingOpen = false
        }
    }
}