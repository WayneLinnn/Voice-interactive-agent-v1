package com.waynelinnn.voiceagent.llm.providers

import com.waynelinnn.voiceagent.BuildConfig
import com.waynelinnn.voiceagent.data.remote.NetworkConfig
import com.waynelinnn.voiceagent.domain.llm.LlmProvider
import com.waynelinnn.voiceagent.domain.model.LlmProviderId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiProvider @Inject constructor() : LlmProvider {
    override val id: String = LlmProviderId.OpenAI.id
    override val baseUrl: String = NetworkConfig.DEFAULT_BASE_URL
    override val chatCompletionsPath: String = NetworkConfig.CHAT_COMPLETIONS_PATH

    override fun apiKey(): String? =
        BuildConfig.OPENAI_API_KEY.takeIf { it.isNotBlank() }

    override fun missingKeyMessage(): String =
        "OpenAI API key missing. Set OPENAI_API_KEY in project-root .env and rebuild."

    override fun supportsModel(modelId: String): Boolean =
        modelId == "gpt-4o-mini" || modelId == "gpt-4o"
}
