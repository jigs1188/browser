package dev.mer.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight HTTP client for Google Gemini free-tier API.
 *
 * Uses raw HttpURLConnection to avoid adding OkHttp/Retrofit dependencies
 * for a single endpoint. This keeps the APK small and dependencies minimal.
 *
 * Rate limits (Gemini free tier as of 2026):
 * - gemini-2.0-flash: 15 RPM, 1M TPM, 1500 RPD
 * - gemini-2.0-flash-lite: 30 RPM, 1M TPM, 1500 RPD
 *
 * We use flash-lite by default for speed and higher rate limits.
 */
@Singleton
class GeminiClient @Inject constructor() {

    companion object {
        private const val TAG = "GeminiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val DEFAULT_MODEL = "gemini-2.0-flash-lite"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    var apiKey: String = ""

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    /**
     * Send a prompt to Gemini and return the text response.
     * Returns Result.failure if API key is missing, network fails, or API errors.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        model: String = DEFAULT_MODEL,
        maxTokens: Int = 1024
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(
                AiException("Gemini API key not configured. Set it in Settings.")
            )
        }

        try {
            val url = URL("$BASE_URL/$model:generateContent?key=$apiKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
            }

            // Build request body
            val requestBody = buildRequestBody(prompt, systemInstruction, maxTokens)

            connection.outputStream.bufferedWriter().use { it.write(requestBody) }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Gemini API error $responseCode: $errorBody")
                return@withContext Result.failure(
                    AiException("Gemini API error ($responseCode): ${parseErrorMessage(errorBody)}")
                )
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val text = parseResponseText(responseBody)
                ?: return@withContext Result.failure(AiException("Empty response from Gemini"))

            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed: ${e.message}", e)
            Result.failure(AiException("AI request failed: ${e.message}", e))
        }
    }

    private fun buildRequestBody(
        prompt: String,
        systemInstruction: String?,
        maxTokens: Int
    ): String {
        val parts = mutableListOf<String>()

        // System instruction (if provided)
        if (systemInstruction != null) {
            parts.add("""
                "systemInstruction": {
                    "parts": [{"text": ${json.encodeToString(systemInstruction)}}]
                }
            """.trimIndent())
        }

        // User content
        parts.add("""
            "contents": [{
                "parts": [{"text": ${json.encodeToString(prompt)}}]
            }]
        """.trimIndent())

        // Generation config
        parts.add("""
            "generationConfig": {
                "maxOutputTokens": $maxTokens,
                "temperature": 0.7
            }
        """.trimIndent())

        return "{ ${parts.joinToString(", ")} }"
    }

    private fun parseResponseText(responseBody: String): String? {
        return try {
            val root = json.decodeFromString<JsonObject>(responseBody)
            root["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response: ${e.message}")
            null
        }
    }

    private fun parseErrorMessage(errorBody: String): String {
        return try {
            val root = json.decodeFromString<JsonObject>(errorBody)
            root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                ?: "Unknown error"
        } catch (e: Exception) {
            errorBody.take(200)
        }
    }
}

class AiException(message: String, cause: Throwable? = null) : Exception(message, cause)
