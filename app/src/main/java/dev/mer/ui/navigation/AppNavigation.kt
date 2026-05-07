package dev.mer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.mer.ui.browser.BrowserScreen
import dev.mer.ui.extensions.ExtensionDetailScreen
import dev.mer.ui.extensions.ExtensionListScreen
import dev.mer.ui.history.HistoryScreen
import dev.mer.ui.settings.SettingsScreen

object Routes {
    const val BROWSER = "browser"
    const val EXTENSIONS = "extensions"
    const val EXTENSION_DETAIL = "extension/{extensionId}"
    const val SETTINGS = "settings"
    const val HISTORY = "history"

    fun extensionDetail(id: String) = "extension/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.BROWSER
    ) {
        composable(Routes.BROWSER) { backStackEntry ->
            val navigateToUrl = backStackEntry.savedStateHandle.get<String>("navigateToUrl")
            
            BrowserScreen(
                initialUrl = navigateToUrl,
                onNavigateToExtensions = {
                    navController.navigate(Routes.EXTENSIONS)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
            
            // Consume the state so it doesn't navigate again on recompose
            backStackEntry.savedStateHandle.remove<String>("navigateToUrl")
        }

        composable(Routes.EXTENSIONS) {
            ExtensionListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { extensionId ->
                    navController.navigate(Routes.extensionDetail(extensionId))
                }
            )
        }

        composable(Routes.EXTENSION_DETAIL) { backStackEntry ->
            val extensionId = backStackEntry.arguments?.getString("extensionId") ?: ""
            ExtensionDetailScreen(
                extensionId = extensionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUrlSelected = { url ->
                    navController.popBackStack()
                    // The BrowserScreen will receive the URL via savedStateHandle
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("navigateToUrl", url)
                }
            )
        }
    }
}
