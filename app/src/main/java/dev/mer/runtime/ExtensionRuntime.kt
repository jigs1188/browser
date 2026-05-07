package dev.mer.runtime

import android.content.Context
import android.util.Log
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mer.ai.AiService
import dev.mer.extension.model.Extension
import dev.mer.extension.model.Permission
import dev.mer.extension.repository.ExtensionRepository
import dev.mer.runtime.bridge.AiBridge
import dev.mer.runtime.bridge.RuntimeBridge
import dev.mer.storage.ExtensionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Top-level orchestrator for the extension runtime.
 *
 * Responsibilities:
 * - Maintains the list of enabled extensions (via Flow from repository)
 * - Determines which extensions match a given URL
 * - Coordinates bridge setup and script/style injection
 * - Manages the bridge lifecycle for each WebView
 * - Handles document_start vs document_end injection timing
 *
 * INJECTION TIMING:
 * - document_start: bridge + scripts injected in onPageStarted (before DOM is ready)
 * - document_end: bridge + scripts injected in onPageFinished (DOM is ready)
 *
 * If both types exist for the same URL, the bridge is set up at document_start
 * and re-used at document_end (not re-injected).
 */
@Singleton
class ExtensionRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ExtensionRepository,
    private val injectionEngine: InjectionEngine,
    private val urlMatcher: UrlMatcher,
    private val extensionStorage: ExtensionStorage,
    private val aiService: AiService
) {
    companion object {
        private const val TAG = "ExtensionRuntime"
    }

    private val json = Json

    /** Observable stream of enabled extensions for UI consumption */
    val enabledExtensions: Flow<List<Extension>> = repository.observeEnabled()

    /** Bridge.js content loaded from assets, cached in memory */
    private val bridgeJs: String by lazy {
        context.assets.open("bridge.js").bufferedReader().readText()
    }

    /** Track current WebView for AI bridge callback delivery */
    private var currentWebView: WebView? = null

    /** Track whether bridge was already set up for the current page load */
    private var bridgeInstalledForUrl: String? = null

    /**
     * Called when a page starts loading (document_start).
     * Injects extensions that have run_at = "document_start".
     */
    fun onPageStarted(webView: WebView, url: String, enabledExtensions: List<Extension>) {
        currentWebView = webView
        bridgeInstalledForUrl = null // Reset for new page

        val matched = matchExtensions(url, enabledExtensions)
        val startExtensions = matched.filter { it.runAt == "document_start" }

        if (startExtensions.isNotEmpty()) {
            setupBridgeAndInject(webView, url, matched, startExtensions)
        }
    }

    /**
     * Called when a page finishes loading (document_end).
     * Injects extensions that have run_at = "document_end" (default).
     */
    fun onPageLoaded(webView: WebView, url: String, enabledExtensions: List<Extension>) {
        currentWebView = webView

        val matched = matchExtensions(url, enabledExtensions)
        val endExtensions = matched.filter { it.runAt != "document_start" }

        if (endExtensions.isNotEmpty()) {
            setupBridgeAndInject(webView, url, matched, endExtensions)
        }
    }

    private fun matchExtensions(url: String, enabledExtensions: List<Extension>): List<Extension> {
        return enabledExtensions.filter { ext ->
            val patterns = json.decodeFromString<List<String>>(ext.matchPatterns)
            urlMatcher.matches(url, patterns)
        }
    }

    /**
     * Sets up the bridge (if not already set up for this URL) and injects the given extensions.
     *
     * @param allMatched All extensions matching this URL (for bridge permission registry)
     * @param toInject The specific extensions to inject in this call
     */
    private fun setupBridgeAndInject(
        webView: WebView,
        url: String,
        allMatched: List<Extension>,
        toInject: List<Extension>
    ) {
        Log.d(TAG, "Injecting ${toInject.size} extension(s) for URL: $url")

        // Only set up bridge once per page load
        if (bridgeInstalledForUrl != url) {
            val bridge = RuntimeBridge(extensionStorage)
            bridge.clearRegistrations()

            val permissionsMap = mutableMapOf<String, Set<Permission>>()
            allMatched.forEach { ext ->
                val permissions = parsePermissions(ext.permissions)
                bridge.registerExtension(ext.id, permissions)
                permissionsMap[ext.id] = permissions
            }

            // Replace previous bridges
            try { webView.removeJavascriptInterface(RuntimeBridge.BRIDGE_NAME) } catch (_: Exception) {}
            try { webView.removeJavascriptInterface(AiBridge.BRIDGE_NAME) } catch (_: Exception) {}

            webView.addJavascriptInterface(bridge, RuntimeBridge.BRIDGE_NAME)

            // Set up AI bridge if any extension has AI permission
            val anyHasAi = permissionsMap.values.any { Permission.AI in it }
            if (anyHasAi && aiService.isConfigured) {
                val aiBridge = AiBridge(
                    aiService = aiService,
                    extensionPermissions = permissionsMap,
                    webViewProvider = { currentWebView }
                )
                webView.addJavascriptInterface(aiBridge, AiBridge.BRIDGE_NAME)
            }

            // Inject bridge.js
            injectionEngine.injectBridge(webView, bridgeJs)
            bridgeInstalledForUrl = url
        }

        // Inject the extension scripts/styles
        toInject.forEach { ext ->
            injectionEngine.injectWithContext(webView, ext)
        }
    }

    // --- Browser-level AI actions ---

    suspend fun summarizePage(pageText: String, url: String): Result<String> {
        return aiService.summarize(pageText, url)
    }

    suspend fun askAboutPage(question: String, pageText: String, url: String): Result<String> {
        return aiService.askAboutPage(question, pageText, url)
    }

    suspend fun explainText(text: String, url: String): Result<String> {
        return aiService.explain(text, url)
    }

    private fun parsePermissions(permissionsJson: String): Set<Permission> {
        return try {
            val permList = json.decodeFromString<List<String>>(permissionsJson)
            permList.mapNotNull { Permission.fromKey(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
