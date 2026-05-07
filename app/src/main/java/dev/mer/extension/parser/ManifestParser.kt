package dev.mer.extension.parser

import dev.mer.extension.model.ExtensionManifest
import dev.mer.extension.model.Permission
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses and validates extension manifest JSON.
 *
 * Validation rules are intentionally strict for security:
 * - Name and version are required and non-empty
 * - At least one URL match pattern is required
 * - All permissions must be recognized
 * - Referenced files existence is NOT checked here (that's ExtensionLoader's job)
 */
@Singleton
class ManifestParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true  // Forward compatibility: ignore fields we don't know
        isLenient = false          // Strict JSON parsing
    }

    fun parse(jsonString: String): Result<ExtensionManifest> {
        return try {
            val manifest = json.decodeFromString<ExtensionManifest>(jsonString)
            validate(manifest)
        } catch (e: Exception) {
            Result.failure(ManifestParseException("Invalid manifest JSON: ${e.message}", e))
        }
    }

    private fun validate(manifest: ExtensionManifest): Result<ExtensionManifest> {
        val errors = mutableListOf<String>()

        // Required fields
        if (manifest.name.isBlank()) {
            errors.add("'name' is required and cannot be empty")
        }
        if (manifest.name.length > 64) {
            errors.add("'name' must be 64 characters or fewer")
        }
        if (manifest.version.isBlank()) {
            errors.add("'version' is required and cannot be empty")
        }

        // URL patterns
        if (manifest.matches.isEmpty()) {
            errors.add("'matches' must contain at least one URL pattern")
        }
        manifest.matches.forEach { pattern ->
            if (!isValidUrlPattern(pattern)) {
                errors.add("Invalid URL pattern: '$pattern'")
            }
        }

        // Permissions
        manifest.permissions.forEach { perm ->
            if (!Permission.isKnown(perm)) {
                errors.add("Unknown permission: '$perm'")
            }
        }

        // Scripts or styles — at least one must be present
        if (manifest.scripts.isEmpty() && manifest.styles.isEmpty()) {
            errors.add("Extension must declare at least one script or style")
        }

        // run_at validation
        if (manifest.runAt !in listOf("document_start", "document_end")) {
            errors.add("'run_at' must be 'document_start' or 'document_end'")
        }

        return if (errors.isEmpty()) {
            Result.success(manifest)
        } else {
            Result.failure(ManifestParseException(errors.joinToString("; ")))
        }
    }

    /**
     * Validates URL match patterns. Supports a simplified Chrome-like format:
     * scheme://host/path where:
     *   scheme = "*" | "http" | "https"
     *   host = "*" | "*.domain.tld" | "domain.tld"
     *   path = anything (wildcards allowed)
     */
    private fun isValidUrlPattern(pattern: String): Boolean {
        if (pattern == "<all_urls>") return true

        val schemeEnd = pattern.indexOf("://")
        if (schemeEnd == -1) return false

        val scheme = pattern.substring(0, schemeEnd)
        if (scheme !in listOf("*", "http", "https")) return false

        val rest = pattern.substring(schemeEnd + 3)
        val pathStart = rest.indexOf('/')
        if (pathStart == -1) return false

        val host = rest.substring(0, pathStart)
        if (host.isEmpty()) return false

        return true
    }
}

class ManifestParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
