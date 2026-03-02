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
    val density = LocalDensity.current

    return remember(windowInfo.containerSize, density) {

        val widthDp = with(density) {
            windowInfo.containerSize.width.toDp().value.toInt()
        }

        val heightDp = with(density) {
            windowInfo.containerSize.height.toDp().value.toInt()
        }

        PlatformConfiguration(
            screenWidthDp = widthDp,
            screenHeightDp = heightDp
        )
    }
}