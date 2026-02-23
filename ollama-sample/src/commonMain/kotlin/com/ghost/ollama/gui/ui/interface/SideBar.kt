package com.ghost.ollama.gui.ui.`interface`

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.ghost.ollama.gui.GetSessionsPaged
import com.ghost.ollama.gui.ui.viewmodel.SessionUiState
import kotlinx.coroutines.flow.Flow
import ollama_kmp.ollama_sample.generated.resources.Res
import ollama_kmp.ollama_sample.generated.resources.chat_dashed
import ollama_kmp.ollama_sample.generated.resources.person
import ollama_kmp.ollama_sample.generated.resources.side_navigation
import org.jetbrains.compose.resources.painterResource


@Composable
fun SideBar(
    modifier: Modifier = Modifier,
    state: SessionUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    when (state) {
        is SessionUiState.Error -> Text(state.message)
        SessionUiState.Loading -> CircularProgressIndicator()
        is SessionUiState.Success -> {
            SidebarContent(
                modifier = modifier,
                isExpanded = expanded,
                onToggle = onToggle,
                sessions = state.pagedSessions,
                searchQuery = state.searchQuery,
                selectedIds = state.selectedSessionIds,
            )
        }
    }
}

@Composable
fun SidebarContent(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    sessions: Flow<PagingData<GetSessionsPaged>>,
    searchQuery: String,
    selectedIds: Set<String>,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ollama", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onToggle) {
                    Icon(painterResource(Res.drawable.side_navigation), contentDescription = "Close Menu")
                }
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            // Navigation items go here in the future
            NavigationDrawerItem(
                label = { Text("Chats") },
                icon = { Icon(painterResource(Res.drawable.chat_dashed), contentDescription = null) },
                selected = true,
                onClick = { },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onToggle) {
                Icon(painterResource(Res.drawable.side_navigation), contentDescription = "Open Menu")
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Navigation items go here in the future
            NavigationRailItem(
                selected = true,
                onClick = { },
                icon = { Icon(painterResource(Res.drawable.person), contentDescription = "Chats") },
                label = { Text("Chats") }
            )
        }
    }
}