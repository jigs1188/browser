package dev.mer.runtime

import android.util.Log
import android.webkit.WebView
import dev.mer.extension.model.Extension
import dev.mer.storage.ExtensionFileManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles JavaScript and CSS injection into WebViews.
 *
 * Injection order for each extension:
 * 1. Context switch: `mer.__setContext(extensionId)`
 * 2. CSS files (injected as <style> elements via JS)
 * 3. JS files (wrapped in IIFE for scope isolation)
 *
 * The bridge.js is injected separately by ExtensionRuntime BEFORE any extension code.
 */
@Singleton
class InjectionEngine @Inject constructor(
    private val fileManager: ExtensionFileManager
) {
    companion object {
        private const val TAG = "InjectionEngine"
    }

    /**
     * Injects all scripts and styles for an extension into the WebView,
     * after switching the bridge context to this extension's ID.
     * Must be called on the main thread (WebView requirement).
     */
    fun injectWithContext(webView: WebView, extension: Extension) {
        try {
            // Switch bridge context to this extension before injecting its code
            val contextSwitch = "mer.__setContext('${escapeJsString(extension.id)}');"
            webView.evaluateJavascript(contextSwitch, null)

            // Inject CSS first — styles should apply before scripts modify DOM
            val styles = kotlinx.serialization.json.Json.decodeFromString<List<String>>(extension.styles)
            styles.forEach { styleFile ->
                injectCss(webView, extension.id, styleFile)
            }

            // Then inject JS — each script wrapped in IIFE
            val scripts = kotlinx.serialization.json.Json.decodeFromString<List<String>>(extension.scripts)
            scripts.forEach { scriptFile ->
                injectJs(webView, extension.id, scriptFile)
            }

            Log.d(TAG, "Injected ${styles.size} styles, ${scripts.size} scripts for '${extension.name}'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject extension '${extension.name}': ${e.message}", e)
        }
    }

    /**
     * Injects the bridge.js runtime API into the WebView.
     * This MUST be called before any extension scripts.
     */
    fun injectBridge(webView: WebView, bridgeJs: String) {
        webView.evaluateJavascript(bridgeJs, null)
        Log.d(TAG, "Bridge injected")
    }

    private fun injectCss(webView: WebView, extensionId: String, fileName: String) {
        val cssContent = fileManager.readFile(extensionId, fileName) ?: run {
            Log.w(TAG, "CSS file not found: $fileName for extension $extensionId")
            return
        }

        // Escape for JavaScript string embedding
        val escaped = cssContent
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")

        val injection = """
            (function() {
                var style = document.createElement('style');
                style.setAttribute('data-mer-ext', '${escapeJsString(extensionId)}');
                style.textContent = '$escaped';
                (document.head || document.documentElement).appendChild(style);
            })();
        """.trimIndent()

        webView.evaluateJavascript(injection, null)
    }

    private fun injectJs(webView: WebView, extensionId: String, fileName: String) {
        val jsContent = fileManager.readFile(extensionId, fileName) ?: run {
            Log.w(TAG, "JS file not found: $fileName for extension $extensionId")
            return
        }

        // Wrap in IIFE to prevent global scope pollution
        val injection = "(function() {\n$jsContent\n})();"
        webView.evaluateJavascript(injection, null)
    }

    /** Escape a string for safe embedding inside single-quoted JS strings */
    private fun escapeJsString(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
    }
}
