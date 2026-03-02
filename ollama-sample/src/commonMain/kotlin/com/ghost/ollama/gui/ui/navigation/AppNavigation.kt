package com.ghost.ollama.gui.ui.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ghost.ollama.gui.ui.`interface`.DownloadScreen
import com.ghost.ollama.gui.ui.`interface`.chat.ChatScreen
import com.ghost.ollama.gui.viewmodel.download.DownloadViewModel
import org.koin.compose.viewmodel.koinViewModel

object Routes {
    const val CHAT = "chat"
    const val DOWNLOAD = "download"
}

@Composable
fun NavigationRoute(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val downloadViewModel: DownloadViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.CHAT,
        modifier = modifier
    ) {

        composable(Routes.CHAT) {
            Surface {
                ChatScreen(
                    onDownloadButtonClick = {
                        navController.navigate(Routes.DOWNLOAD)
                    },
                )
            }
        }

        composable(Routes.DOWNLOAD) {
            DownloadScreen(
                viewModel = downloadViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}