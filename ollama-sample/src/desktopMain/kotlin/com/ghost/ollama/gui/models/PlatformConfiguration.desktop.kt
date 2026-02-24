package com.ghost.ollama.gui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

actual class PlatformConfiguration(
    actual val screenWidthDp: Int,
    actual val screenHeightDp: Int
) {
    actual val isWideScreen: Boolean = screenWidthDp >= 600
}

@Composable
actual fun rememberPlatformConfiguration(): PlatformConfiguration {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current.density
    return remember(windowInfo) {
        val widthPx = windowInfo.containerSize.width
        val heightPx = windowInfo.containerSize.height

        // Convert pixels to dp (simplified - you might want more precise conversion)
        PlatformConfiguration(
            screenWidthDp = (widthPx / density).toInt(),
            screenHeightDp = (heightPx / density).toInt()
        )
    }
}