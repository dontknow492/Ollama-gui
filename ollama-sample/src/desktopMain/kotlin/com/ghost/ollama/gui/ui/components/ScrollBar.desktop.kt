package com.ghost.ollama.gui.ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun BoxScope.ChatScrollbar(
    listState: LazyListState,
    reverseLayout: Boolean,
    modifier: Modifier
) {
    val scrollbarAdapter = rememberScrollbarAdapter(listState)

    CompositionLocalProvider(
        LocalScrollbarStyle provides ScrollbarStyle(
            minimalHeight = 24.dp,
            thickness = 12.dp,
            shape = RoundedCornerShape(6.dp),
            hoverDurationMillis = 300,
            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    ) {
        VerticalScrollbar(
            modifier = modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            reverseLayout = reverseLayout,
            adapter = scrollbarAdapter
        )
    }
}