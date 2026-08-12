package com.waynelinnn.voiceagent.domain.llm

/**
 * Vendor-specific LLM endpoint + credentials.
 * Shared transport stays OpenAI-compatible; providers only differ by URL/key/models.
 */
interface LlmProvider {
    val id: String
    val baseUrl: String
    val chatCompletionsPath: String

    fun apiKey(): String?
    fun missingKeyMessage(): String
    fun supportsModel(modelId: String): Boolean
}
