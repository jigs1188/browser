package dev.mer.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.mer.ui.components.UrlBar

@Composable
fun BrowserScreen(
    initialUrl: String? = null,
    onNavigateToExtensions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val enabledExtensions by viewModel.enabledExtensions.collectAsState()
    val aiOverlayState by viewModel.aiOverlayState.collectAsState()

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.onUrlSubmitted(initialUrl)
        }
    }

    // Handle system back button
    BackHandler {
        if (aiOverlayState.isVisible) {
            viewModel.dismissAiOverlay()
        } else {
            viewModel.goBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab strip
            TabBar(
                tabs = uiState.tabs,
                activeTabIndex = uiState.activeTabIndex,
                onTabClick = viewModel::switchTab,
                onTabClose = viewModel::closeTab,
                onAddTab = viewModel::addTab
            )

            // URL bar with navigation controls
            UrlBar(
                urlText = uiState.urlBarText,
                isLoading = uiState.activeTab.isLoading,
                progress = uiState.activeTab.progress,
                extensionCount = enabledExtensions.size,
                isAiConfigured = viewModel.isAiConfigured,
                onTextChanged = viewModel::onUrlBarTextChanged,
                onSubmit = viewModel::onUrlSubmitted,
                onFocusChanged = viewModel::onUrlBarFocusChanged,
                onBackClick = viewModel::goBack,
                onForwardClick = viewModel::goForward,
                onRefreshClick = viewModel::refresh,
                onExtensionsClick = onNavigateToExtensions,
                onAiClick = viewModel::toggleAiOverlay,
                onSettingsClick = onNavigateToSettings,
                onHistoryClick = onNavigateToHistory,
                onBookmarksClick = onNavigateToBookmarks,
                onAddBookmark = { url, _ -> viewModel.addBookmark(url, uiState.activeTab.title) },
                modifier = Modifier.fillMaxWidth()
            )

            // WebView
            WebViewContainer(
                modifier = Modifier.fillMaxSize(),
                onWebViewCreated = viewModel::onWebViewCreated,
                onPageStarted = viewModel::onPageStarted,
                onPageFinished = viewModel::onPageFinished,
                onProgressChanged = viewModel::onProgressChanged,
                onTitleChanged = viewModel::onTitleChanged
            )
        }

        // AI overlay — floats above WebView
        AiOverlay(
            state = aiOverlayState,
            onQueryChanged = viewModel::onAiQueryChanged,
            onAsk = viewModel::askAboutPage,
            onSummarize = viewModel::summarizePage,
            onDismiss = viewModel::dismissAiOverlay,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}
