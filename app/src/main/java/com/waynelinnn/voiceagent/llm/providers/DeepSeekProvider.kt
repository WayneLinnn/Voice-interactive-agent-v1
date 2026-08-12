package com.waynelinnn.voiceagent.llm.providers

import com.waynelinnn.voiceagent.BuildConfig
import com.waynelinnn.voiceagent.data.remote.NetworkConfig
import com.waynelinnn.voiceagent.domain.llm.LlmProvider
import com.waynelinnn.voiceagent.domain.model.LlmProviderId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek OpenAI-compatible Chat Completions API.
 * @see https://api.deepseek.com
 */
@Singleton
class DeepSeekProvider @Inject constructor() : LlmProvider {
    override val id: String = LlmProviderId.DeepSeek.id
    override val baseUrl: String = "https://api.deepseek.com/"
    override val chatCompletionsPath: String = NetworkConfig.CHAT_COMPLETIONS_PATH

    override fun apiKey(): String? =
        BuildConfig.DEEPSEEK_API_KEY.takeIf { it.isNotBlank() }

    override fun missingKeyMessage(): String =
        "DeepSeek API key missing. Set DEEPSEEK_API_KEY in project-root .env and rebuild."

    override fun supportsModel(modelId: String): Boolean =
        modelId == "deepseek-chat" || modelId == "deepseek-reasoner"
}
