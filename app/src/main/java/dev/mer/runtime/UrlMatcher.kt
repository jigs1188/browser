package dev.mer.runtime

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Matches URLs against Chrome-like match patterns.
 *
 * Supported pattern formats:
 *   scheme://host/path — where scheme, host parts can use wildcards
 *   Special value: all_urls (in angle brackets) — matches everything
 *
 * Implementation converts glob patterns to regex at match time.
 * For MVP, we don't cache compiled patterns — the extension count is small enough
 * that this won't matter. Optimize if profiling shows it's a bottleneck.
 */
@Singleton
class UrlMatcher @Inject constructor() {

    fun matches(url: String, patterns: List<String>): Boolean {
        return patterns.any { pattern -> matchesPattern(url, pattern) }
    }

    private fun matchesPattern(url: String, pattern: String): Boolean {
        if (pattern == "<all_urls>") return true

        try {
            val schemeEnd = pattern.indexOf("://")
            if (schemeEnd == -1) return false

            val scheme = pattern.substring(0, schemeEnd)
            val rest = pattern.substring(schemeEnd + 3)

            val pathStart = rest.indexOf('/')
            if (pathStart == -1) return false

            val hostPattern = rest.substring(0, pathStart)
            val pathPattern = rest.substring(pathStart)

            // Parse the URL being tested
            val urlSchemeEnd = url.indexOf("://")
            if (urlSchemeEnd == -1) return false

            val urlScheme = url.substring(0, urlSchemeEnd)
            val urlRest = url.substring(urlSchemeEnd + 3)

            val urlPathStart = urlRest.indexOf('/')
            val urlHost: String
            val urlPath: String

            if (urlPathStart == -1) {
                urlHost = urlRest
                urlPath = "/"
            } else {
                urlHost = urlRest.substring(0, urlPathStart)
                urlPath = urlRest.substring(urlPathStart)
            }

            // Match scheme
            if (scheme != "*" && scheme != urlScheme) return false

            // Match host
            if (!matchesHost(urlHost, hostPattern)) return false

            // Match path
            if (!matchesGlob(urlPath, pathPattern)) return false

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun matchesHost(host: String, pattern: String): Boolean {
        if (pattern == "*") return true

        if (pattern.startsWith("*.")) {
            val baseDomain = pattern.substring(2)
            return host == baseDomain || host.endsWith(".$baseDomain")
        }

        return host == pattern
    }

    private fun matchesGlob(text: String, pattern: String): Boolean {
        // Convert glob pattern to regex
        val regex = buildString {
            append("^")
            for (char in pattern) {
                when (char) {
                    '*' -> append(".*")
                    '?' -> append(".")
                    '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> {
                        append("\\")
                        append(char)
                    }
                    else -> append(char)
                }
            }
            append("$")
        }
        return Regex(regex).matches(text)
    }
}
