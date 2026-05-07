package dev.mer.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level AI service for browser-specific operations.
 *
 * Provides pre-configured prompts for common webpage augmentation tasks.
 * All methods return plain text — the UI/bridge layer handles formatting.
 *
 * This abstraction exists so we can swap Gemini for another provider later
 * without changing 15 call sites. For now there's only one implementation,
 * which is fine — premature interface extraction is worse than a concrete class.
 */
@Singleton
class AiService @Inject constructor(
    private val geminiClient: GeminiClient
) {
    companion object {
        private const val SYSTEM_INSTRUCTION = """You are a helpful AI assistant integrated into a mobile browser. 
You help users understand, summarize, and interact with web page content.
Be concise — mobile screen space is limited. Use short paragraphs.
Never generate harmful, misleading, or inappropriate content.
When summarizing, focus on key facts and actionable information."""
    }

    val isConfigured: Boolean
        get() = geminiClient.isConfigured

    fun setApiKey(key: String) {
        geminiClient.apiKey = key
    }

    fun getApiKey(): String = geminiClient.apiKey

    /**
     * Summarize webpage content.
     * @param pageText Extracted text from the page (truncated to avoid token limits)
     * @param url The page URL for context
     */
    suspend fun summarize(pageText: String, url: String): Result<String> {
        val truncated = pageText.take(8000) // ~2K tokens, stay within free tier
        val prompt = """Summarize this webpage concisely in 3-5 bullet points.

URL: $url

Page content:
$truncated"""

        return geminiClient.generateContent(
            prompt = prompt,
            systemInstruction = SYSTEM_INSTRUCTION,
            maxTokens = 512
        )
    }

    /**
     * Answer a question about webpage content.
     */
    suspend fun askAboutPage(question: String, pageText: String, url: String): Result<String> {
        val truncated = pageText.take(6000)
        val prompt = """Answer this question about the webpage. Be concise and helpful.

URL: $url
Question: $question

Page content:
$truncated"""

        return geminiClient.generateContent(
            prompt = prompt,
            systemInstruction = SYSTEM_INSTRUCTION,
            maxTokens = 512
        )
    }

    /**
     * Explain highlighted/selected text in simpler terms.
     */
    suspend fun explain(selectedText: String, pageUrl: String): Result<String> {
        val prompt = """Explain this text in simple, clear language. Keep it short (2-3 sentences).

Context: from $pageUrl
Text: "$selectedText" """

        return geminiClient.generateContent(
            prompt = prompt,
            systemInstruction = SYSTEM_INSTRUCTION,
            maxTokens = 256
        )
    }

    /**
     * General AI query — used by extensions via mer.ai.ask()
     */
    suspend fun ask(prompt: String): Result<String> {
        return geminiClient.generateContent(
            prompt = prompt,
            systemInstruction = SYSTEM_INSTRUCTION,
            maxTokens = 512
        )
    }

    /**
     * Extract structured data from page content.
     * Returns the extracted text — callers parse it as needed.
     */
    suspend fun extract(pageText: String, instruction: String): Result<String> {
        val truncated = pageText.take(6000)
        val prompt = """Extract the following from this webpage content.
Instruction: $instruction

Page content:
$truncated"""

        return geminiClient.generateContent(
            prompt = prompt,
            systemInstruction = SYSTEM_INSTRUCTION,
            maxTokens = 512
        )
    }
}
