package com.waynelinnn.voiceagent.llm

import com.waynelinnn.voiceagent.data.llm.OpenAiCompatibleChatTransport
import com.waynelinnn.voiceagent.domain.llm.LlmClient
import com.waynelinnn.voiceagent.domain.llm.LlmProvider
import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmModelCatalog
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Course-style unified LLM entry (like `export const llmClient`).
 * Routes by model catalog → provider → shared OpenAI-compatible transport.
 */
@Singleton
class RoutingLlmClient @Inject constructor(
    private val transport: OpenAiCompatibleChatTransport,
    providers: Set<@JvmSuppressWildcards LlmProvider>,
) : LlmClient {

    override val providerId: String = "routing"

    private val providersById: Map<String, LlmProvider> =
        providers.associateBy { it.id }

    override fun streamChat(request: LlmChatRequest): Flow<LlmStreamEvent> {
        val option = LlmModelCatalog.models.firstOrNull { it.id == request.modelId }
            ?: return flow {
                emit(LlmStreamEvent.Error("Unknown model \"${request.modelId}\"."))
            }
        if (!option.enabled) {
            return flow {
                emit(
                    LlmStreamEvent.Error(
                        "Model \"${request.modelId}\" is not enabled yet.",
                    ),
                )
            }
        }
        val provider = providersById[option.provider.id]
            ?: return flow {
                emit(
                    LlmStreamEvent.Error(
                        "No provider wired for ${option.provider.displayName}.",
                    ),
                )
            }
        if (!provider.supportsModel(request.modelId)) {
            return flow {
                emit(
                    LlmStreamEvent.Error(
                        "Provider ${provider.id} does not support \"${request.modelId}\".",
                    ),
                )
            }
        }
        return transport.streamChat(provider, request)
    }
}
