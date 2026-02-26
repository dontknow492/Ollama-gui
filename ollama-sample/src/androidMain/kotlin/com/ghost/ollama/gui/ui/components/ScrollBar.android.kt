package com.ghost.ollama.gui.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun BoxScope.ChatScrollbar(
    listState: LazyListState,
    reverseLayout: Boolean,
    modifier: Modifier
) {
    // No-op on Android
    // Android uses system scrollbars automatically
}