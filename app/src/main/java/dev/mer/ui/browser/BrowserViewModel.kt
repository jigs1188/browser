package dev.mer.ui.browser

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mer.ai.AiService
import dev.mer.extension.model.Extension
import dev.mer.runtime.ExtensionRuntime
import dev.mer.storage.HistoryDao
import dev.mer.storage.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TabState(
    val id: Int = 0,
    val url: String = "",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val progress: Int = 0
)

data class BrowserUiState(
    val tabs: List<TabState> = listOf(TabState()),
    val activeTabIndex: Int = 0,
    val urlBarText: String = "",
    val isUrlBarFocused: Boolean = false,
    val extensionCount: Int = 0
) {
    val activeTab: TabState get() = tabs[activeTabIndex]
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val extensionRuntime: ExtensionRuntime,
    private val aiService: AiService,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    /** AI overlay state */
    private val _aiOverlayState = MutableStateFlow(AiOverlayState())
    val aiOverlayState: StateFlow<AiOverlayState> = _aiOverlayState.asStateFlow()

    /** Enabled extensions, collected as StateFlow for injection timing */
    val enabledExtensions: StateFlow<List<Extension>> =
        extensionRuntime.enabledExtensions.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

    private var webViewRef: WebView? = null
    private var nextTabId = 1

    val isAiConfigured: Boolean
        get() = aiService.isConfigured

    fun onWebViewCreated(webView: WebView) {
        webViewRef = webView
    }

    fun onUrlSubmitted(input: String) {
        val url = normalizeUrl(input)
        _uiState.update {
            it.copy(
                urlBarText = url,
                isUrlBarFocused = false
            )
        }
        webViewRef?.loadUrl(url)
    }

    fun onUrlBarTextChanged(text: String) {
        _uiState.update { it.copy(urlBarText = text) }
    }

    fun onUrlBarFocusChanged(focused: Boolean) {
        _uiState.update {
            if (focused) {
                it.copy(isUrlBarFocused = true, urlBarText = it.activeTab.url)
            } else {
                it.copy(isUrlBarFocused = false)
            }
        }
    }

    fun onPageStarted(url: String) {
        _uiState.update {
            val tabs = it.tabs.toMutableList()
            tabs[it.activeTabIndex] = tabs[it.activeTabIndex].copy(
                url = url,
                isLoading = true,
                progress = 0
            )
            it.copy(
                tabs = tabs,
                urlBarText = if (!it.isUrlBarFocused) url else it.urlBarText
            )
        }

        // Trigger document_start extension injection
        webViewRef?.let { wv ->
            extensionRuntime.onPageStarted(wv, url, enabledExtensions.value)
        }
    }

    fun onPageFinished(url: String) {
        _uiState.update {
            val tabs = it.tabs.toMutableList()
            tabs[it.activeTabIndex] = tabs[it.activeTabIndex].copy(
                isLoading = false,
                progress = 100
            )
            it.copy(tabs = tabs)
        }

        // Trigger extension injection
        webViewRef?.let { wv ->
            extensionRuntime.onPageLoaded(wv, url, enabledExtensions.value)
        }

        // Record history
        val title = _uiState.value.activeTab.title
        if (url.isNotBlank() && !url.startsWith("file://") && !url.startsWith("about:")) {
            viewModelScope.launch {
                historyDao.insert(HistoryEntry(url = url, title = title))
            }
        }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update {
            val tabs = it.tabs.toMutableList()
            tabs[it.activeTabIndex] = tabs[it.activeTabIndex].copy(progress = progress)
            it.copy(tabs = tabs)
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update {
            val tabs = it.tabs.toMutableList()
            tabs[it.activeTabIndex] = tabs[it.activeTabIndex].copy(title = title)
            it.copy(tabs = tabs)
        }
    }

    fun goBack() {
        webViewRef?.let { if (it.canGoBack()) it.goBack() }
    }

    fun goForward() {
        webViewRef?.let { if (it.canGoForward()) it.goForward() }
    }

    fun refresh() {
        webViewRef?.reload()
    }

    fun addTab() {
        _uiState.update {
            val newTab = TabState(id = nextTabId++)
            it.copy(
                tabs = it.tabs + newTab,
                activeTabIndex = it.tabs.size,
                urlBarText = ""
            )
        }
    }

    fun switchTab(index: Int) {
        if (index in _uiState.value.tabs.indices) {
            _uiState.update {
                it.copy(
                    activeTabIndex = index,
                    urlBarText = it.tabs[index].url
                )
            }
        }
    }

    fun closeTab(index: Int) {
        _uiState.update {
            if (it.tabs.size <= 1) return@update it // Don't close last tab

            val tabs = it.tabs.toMutableList()
            tabs.removeAt(index)
            val newActiveIndex = when {
                index < it.activeTabIndex -> it.activeTabIndex - 1
                index == it.activeTabIndex -> minOf(index, tabs.size - 1)
                else -> it.activeTabIndex
            }
            it.copy(tabs = tabs, activeTabIndex = newActiveIndex)
        }
    }

    // --- AI Overlay ---

    fun toggleAiOverlay() {
        _aiOverlayState.update {
            it.copy(isVisible = !it.isVisible)
        }
    }

    fun dismissAiOverlay() {
        _aiOverlayState.update {
            AiOverlayState(isVisible = false)
        }
    }

    fun onAiQueryChanged(text: String) {
        _aiOverlayState.update { it.copy(queryText = text) }
    }

    fun askAboutPage(question: String) {
        _aiOverlayState.update { it.copy(isLoading = true, error = null, response = "") }

        viewModelScope.launch {
            // Extract page text via JS
            val pageText = extractPageText()
            val url = _uiState.value.activeTab.url

            val result = extensionRuntime.askAboutPage(question, pageText, url)
            result.fold(
                onSuccess = { response ->
                    _aiOverlayState.update {
                        it.copy(isLoading = false, response = response, queryText = "")
                    }
                },
                onFailure = { error ->
                    _aiOverlayState.update {
                        it.copy(isLoading = false, error = error.message ?: "AI request failed")
                    }
                }
            )
        }
    }

    fun summarizePage() {
        _aiOverlayState.update { it.copy(isLoading = true, error = null, response = "") }

        viewModelScope.launch {
            val pageText = extractPageText()
            val url = _uiState.value.activeTab.url

            val result = extensionRuntime.summarizePage(pageText, url)
            result.fold(
                onSuccess = { response ->
                    _aiOverlayState.update {
                        it.copy(isLoading = false, response = response)
                    }
                },
                onFailure = { error ->
                    _aiOverlayState.update {
                        it.copy(isLoading = false, error = error.message ?: "Summarize failed")
                    }
                }
            )
        }
    }

    /**
     * Extracts page text from the current WebView.
     * Uses a blocking JS evaluation — safe because viewModelScope is on Main.
     */
    private suspend fun extractPageText(): String {
        val wv = webViewRef ?: return ""
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            wv.evaluateJavascript(
                "(document.body.innerText || '').substring(0, 10000)"
            ) { result ->
                // JS returns the string wrapped in quotes
                val text = result
                    ?.removeSurrounding("\"")
                    ?.replace("\\n", "\n")
                    ?.replace("\\t", "\t")
                    ?: ""
                cont.resumeWith(Result.success(text))
            }
        }
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }
}
