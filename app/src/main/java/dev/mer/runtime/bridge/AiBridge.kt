package dev.mer.runtime.bridge

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import dev.mer.ai.AiService
import dev.mer.extension.model.Permission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bridge for AI-related JavaScript API calls.
 *
 * Exposed to JavaScript as "MerAiNative" via WebView.addJavascriptInterface().
 * The bridge.js wraps this in the `mer.ai` namespace.
 *
 * DESIGN DECISION: Separate bridge object from RuntimeBridge.
 * Reasoning: AI calls are async (network I/O) while storage calls are sync.
 * Keeping them separate prevents a bloated single bridge class and makes
 * the async callback pattern cleaner.
 *
 * The AI bridge uses a callback-based pattern because addJavascriptInterface
 * methods are synchronous — they can't return Promises. Instead, we:
 * 1. Accept a JS callback ID
 * 2. Launch a coroutine to call GeminiClient
 * 3. Evaluate JS `__merAiCallback(id, result, error)` when done
 */
class AiBridge(
    private val aiService: AiService,
    private val extensionPermissions: Map<String, Set<Permission>>,
    private val webViewProvider: () -> WebView?
) {
    companion object {
        private const val TAG = "MerAiBridge"
        const val BRIDGE_NAME = "MerAiNative"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activeExtensionId: String = ""

    @JavascriptInterface
    fun setActiveExtension(extensionId: String) {
        activeExtensionId = extensionId
    }

    @JavascriptInterface
    fun ask(prompt: String, callbackId: String) {
        val extId = activeExtensionId
        if (!hasAiPermission(extId)) {
            deliverError(callbackId, "Extension '$extId' does not have 'ai' permission")
            return
        }

        scope.launch {
            val result = aiService.ask(prompt)
            result.fold(
                onSuccess = { deliverResult(callbackId, it) },
                onFailure = { deliverError(callbackId, it.message ?: "AI request failed") }
            )
        }
    }

    @JavascriptInterface
    fun summarize(pageText: String, url: String, callbackId: String) {
        val extId = activeExtensionId
        if (!hasAiPermission(extId)) {
            deliverError(callbackId, "Extension '$extId' does not have 'ai' permission")
            return
        }

        scope.launch {
            val result = aiService.summarize(pageText, url)
            result.fold(
                onSuccess = { deliverResult(callbackId, it) },
                onFailure = { deliverError(callbackId, it.message ?: "Summarize failed") }
            )
        }
    }

    @JavascriptInterface
    fun explain(selectedText: String, pageUrl: String, callbackId: String) {
        val extId = activeExtensionId
        if (!hasAiPermission(extId)) {
            deliverError(callbackId, "Extension '$extId' does not have 'ai' permission")
            return
        }

        scope.launch {
            val result = aiService.explain(selectedText, pageUrl)
            result.fold(
                onSuccess = { deliverResult(callbackId, it) },
                onFailure = { deliverError(callbackId, it.message ?: "Explain failed") }
            )
        }
    }

    @JavascriptInterface
    fun extract(pageText: String, instruction: String, callbackId: String) {
        val extId = activeExtensionId
        if (!hasAiPermission(extId)) {
            deliverError(callbackId, "Extension '$extId' does not have 'ai' permission")
            return
        }

        scope.launch {
            val result = aiService.extract(pageText, instruction)
            result.fold(
                onSuccess = { deliverResult(callbackId, it) },
                onFailure = { deliverError(callbackId, it.message ?: "Extract failed") }
            )
        }
    }

    @JavascriptInterface
    fun isConfigured(): Boolean = aiService.isConfigured

    private fun hasAiPermission(extensionId: String): Boolean {
        return extensionPermissions[extensionId]?.contains(Permission.AI) == true
    }

    private fun deliverResult(callbackId: String, result: String) {
        val escaped = result
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\"", "\\\"")

        val js = "__merAiCallback('$callbackId', \"$escaped\", null);"
        webViewProvider()?.let { wv ->
            wv.post { wv.evaluateJavascript(js, null) }
        }
    }

    private fun deliverError(callbackId: String, error: String) {
        val escaped = error
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\"", "\\\"")

        val js = "__merAiCallback('$callbackId', null, \"$escaped\");"
        webViewProvider()?.let { wv ->
            wv.post { wv.evaluateJavascript(js, null) }
        }
    }
}
