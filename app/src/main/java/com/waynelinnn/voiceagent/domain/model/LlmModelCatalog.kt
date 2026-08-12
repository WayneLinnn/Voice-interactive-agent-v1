package com.waynelinnn.voiceagent.domain.model

enum class LlmProviderId(val id: String, val displayName: String) {
    OpenAI("openai", "OpenAI"),
    AzureOpenAI("azure_openai", "Azure OpenAI"),
    Anthropic("anthropic", "Anthropic"),
    DeepSeek("deepseek", "DeepSeek"),
}

data class LlmModelOption(
    val id: String,
    val displayName: String,
    val provider: LlmProviderId,
    val enabled: Boolean,
    val tags: List<String> = emptyList(),
)

/**
 * Static catalog. OpenAI + DeepSeek are wired via RoutingLlmClient providers.
 * Selectability also requires a provider API key at runtime (see settings UI).
 */
object LlmModelCatalog {
    val models: List<LlmModelOption> = listOf(
        LlmModelOption(
            id = "gpt-4o-mini",
            displayName = "GPT-4o mini",
            provider = LlmProviderId.OpenAI,
            enabled = true,
            tags = listOf("fast", "default"),
        ),
        LlmModelOption(
            id = "gpt-4o",
            displayName = "GPT-4o",
            provider = LlmProviderId.OpenAI,
            enabled = true,
            tags = listOf("strong"),
        ),
        LlmModelOption(
            id = "azure-gpt-4o-mini",
            displayName = "Azure GPT-4o mini",
            provider = LlmProviderId.AzureOpenAI,
            enabled = false,
            tags = listOf("coming"),
        ),
        LlmModelOption(
            id = "claude-sonnet",
            displayName = "Claude Sonnet",
            provider = LlmProviderId.Anthropic,
            enabled = false,
            tags = listOf("coming"),
        ),
        LlmModelOption(
            id = "deepseek-chat",
            displayName = "DeepSeek Chat",
            provider = LlmProviderId.DeepSeek,
            enabled = true,
            tags = listOf("cn", "value"),
        ),
    )

    val defaultModelId: String = AppSettings.DEFAULT_MODEL_ID

    fun enabledModels(): List<LlmModelOption> = models.filter { it.enabled }

    fun find(modelId: String): LlmModelOption? = models.firstOrNull { it.id == modelId }

    fun isSelectable(modelId: String): Boolean = find(modelId)?.enabled == true

    fun isSelectable(modelId: String, hasProviderKey: (String) -> Boolean): Boolean {
        val model = find(modelId) ?: return false
        return model.enabled && hasProviderKey(model.provider.id)
    }

    fun firstSelectableId(hasProviderKey: (String) -> Boolean): String? =
        models.firstOrNull { it.enabled && hasProviderKey(it.provider.id) }?.id
}
