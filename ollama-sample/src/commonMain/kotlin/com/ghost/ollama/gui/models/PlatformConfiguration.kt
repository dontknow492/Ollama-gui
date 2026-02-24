package com.ghost.ollama.gui.models

import androidx.compose.runtime.Composable

// In commonMain
expect class PlatformConfiguration {
    val screenWidthDp: Int
    val screenHeightDp: Int
    val isWideScreen: Boolean
}

@Composable
expect fun rememberPlatformConfiguration(): PlatformConfiguration