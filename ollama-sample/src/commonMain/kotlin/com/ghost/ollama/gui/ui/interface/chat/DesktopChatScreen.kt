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
import com.ghost.ollama.gui.ui.viewmodel.ChatUiState
import com.ghost.ollama.gui.ui.viewmodel.SessionUiState

@Composable
fun DesktopChatScreen(
    state: ChatUiState,
    sessionState: SessionUiState,
    snackbarHostState: SnackbarHostState,
    onSendMessage: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    modifier: Modifier = Modifier
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
                    onToggle = { isDesktopSidebarExpanded = false }
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
                    onToggle = { isDesktopSidebarExpanded = true }
                )
            }
        }

        ChatMainContent(
            state = state,
            snackbarHostState = snackbarHostState,
            isMobile = false,
            onMenuClick = { }, // Unused on desktop
            onSendMessage = onSendMessage,
            onCopyMessage = onCopyMessage,
            onDeleteMessage = onDeleteMessage,
            modifier = Modifier.weight(1f)
        )
    }
}