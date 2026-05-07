package dev.mer.runtime.bridge

import android.util.Log
import android.webkit.JavascriptInterface
import dev.mer.extension.model.Permission
import dev.mer.storage.ExtensionStorage

/**
 * Kotlin side of the JS ↔ Kotlin bridge.
 *
 * Exposed to JavaScript as "MerNative" via WebView.addJavascriptInterface().
 * All methods run on a background WebView thread — NOT the main thread.
 *
 * ARCHITECTURE NOTE (v0.2):
 * This is now a SINGLE bridge instance per WebView that routes calls
 * by extension ID. Previously, each extension got its own bridge instance,
 * but since addJavascriptInterface uses a single name ("MerNative"),
 * the last-registered bridge overwrote all previous ones. Now the bridge
 * holds a registry of extension permissions and routes storage calls
 * to the correct namespace.
 *
 * Security design:
 * - Storage operations are namespaced by extension ID
 * - Permission checks gate access to each API
 * - Only primitive types and strings cross the bridge (Android limitation)
 */
class RuntimeBridge(
    private val storage: ExtensionStorage
) {
    companion object {
        private const val TAG = "MerBridge"
        const val BRIDGE_NAME = "MerNative"
    }

    // Registry of extension ID → granted permissions
    private val extensionPermissions = mutableMapOf<String, Set<Permission>>()

    // The "active" extension ID — set by bridge.js before each extension's scripts run
    @Volatile
    private var activeExtensionId: String = ""

    /** Register an extension and its permissions before injection */
    fun registerExtension(extensionId: String, permissions: Set<Permission>) {
        extensionPermissions[extensionId] = permissions
    }

    /** Clear all registrations (call before new page injection) */
    fun clearRegistrations() {
        extensionPermissions.clear()
        activeExtensionId = ""
    }

    @JavascriptInterface
    fun setActiveExtension(extensionId: String) {
        // Only allow switching to registered extensions
        if (extensionId in extensionPermissions) {
            activeExtensionId = extensionId
        } else {
            Log.w(TAG, "Attempt to activate unregistered extension: $extensionId")
        }
    }

    @JavascriptInterface
    fun storageGet(key: String): String {
        val extId = activeExtensionId
        return try {
            requirePermission(extId, Permission.STORAGE)
            storage.get(extId, key) ?: "null"
        } catch (e: SecurityException) {
            Log.w(TAG, "[$extId] storage.get denied: ${e.message}")
            "null"
        }
    }

    @JavascriptInterface
    fun storageSet(key: String, value: String) {
        val extId = activeExtensionId
        try {
            requirePermission(extId, Permission.STORAGE)
            storage.set(extId, key, value)
        } catch (e: SecurityException) {
            Log.w(TAG, "[$extId] storage.set denied: ${e.message}")
        }
    }

    @JavascriptInterface
    fun storageRemove(key: String) {
        val extId = activeExtensionId
        try {
            requirePermission(extId, Permission.STORAGE)
            storage.remove(extId, key)
        } catch (e: SecurityException) {
            Log.w(TAG, "[$extId] storage.remove denied: ${e.message}")
        }
    }

    @JavascriptInterface
    fun getExtensionId(): String = activeExtensionId

    @JavascriptInterface
    fun log(level: String, message: String) {
        val extId = activeExtensionId.ifEmpty { "unknown" }
        when (level) {
            "debug" -> Log.d(TAG, "[$extId] $message")
            "info" -> Log.i(TAG, "[$extId] $message")
            "warn" -> Log.w(TAG, "[$extId] $message")
            "error" -> Log.e(TAG, "[$extId] $message")
            else -> Log.d(TAG, "[$extId] $message")
        }
    }

    // --- AI Bridge Methods ---

    @JavascriptInterface
    fun hasPermission(permission: String): Boolean {
        val perm = Permission.fromKey(permission) ?: return false
        return extensionPermissions[activeExtensionId]?.contains(perm) == true
    }

    private fun requirePermission(extensionId: String, permission: Permission) {
        if (extensionId.isEmpty()) {
            throw SecurityException("No active extension context")
        }
        val perms = extensionPermissions[extensionId]
            ?: throw SecurityException("Extension '$extensionId' is not registered")
        if (permission !in perms) {
            throw SecurityException(
                "Extension '$extensionId' does not have '${permission.key}' permission"
            )
        }
    }
}
