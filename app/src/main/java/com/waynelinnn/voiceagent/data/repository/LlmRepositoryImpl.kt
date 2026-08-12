package com.waynelinnn.voiceagent.data.repository

import com.waynelinnn.voiceagent.domain.llm.LlmClient
import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmModelCatalog
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import com.waynelinnn.voiceagent.domain.repository.LlmRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class LlmRepositoryImpl @Inject constructor(
    private val llmClient: LlmClient,
) : LlmRepository {
    override fun streamChat(request: LlmChatRequest): Flow<LlmStreamEvent> {
        if (!LlmModelCatalog.isSelectable(request.modelId)) {
            return flow {
                emit(
                    LlmStreamEvent.Error(
                        "Model \"${request.modelId}\" is not available yet. Pick an enabled model in Model settings.",
                    ),
                )
            }
        }
        return llmClient.streamChat(request)
    }
}
