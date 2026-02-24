package com.ghost.ollama.gui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

actual class PlatformConfiguration(
    actual val screenWidthDp: Int,
    actual val screenHeightDp: Int
) {
    actual val isWideScreen: Boolean = screenWidthDp >= 600
}

@Composable
actual fun rememberPlatformConfiguration(): PlatformConfiguration {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        PlatformConfiguration(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp
        )
    }
}