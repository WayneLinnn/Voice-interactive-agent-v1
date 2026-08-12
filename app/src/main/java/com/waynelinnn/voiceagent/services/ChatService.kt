package com.waynelinnn.voiceagent.services

import com.waynelinnn.voiceagent.domain.model.LlmChatMessage
import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import com.waynelinnn.voiceagent.domain.model.MessageRole
import com.waynelinnn.voiceagent.domain.repository.LlmRepository
import com.waynelinnn.voiceagent.llm.prompts.PromptCatalog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Course-style service layer (like `chat.service.ts`).
 * Orchestrates prompts + LLM repository; UI/VoiceSession stay thin.
 */
@Singleton
class ChatService @Inject constructor(
    private val llmRepository: LlmRepository,
    private val promptCatalog: PromptCatalog,
) {
    fun streamChat(
        modelId: String,
        history: List<LlmChatMessage>,
    ): Flow<LlmStreamEvent> {
        val request = LlmChatRequest(
            modelId = modelId,
            messages = listOf(
                LlmChatMessage(
                    role = MessageRole.System,
                    content = promptCatalog.voiceAssistantSystemPrompt(),
                ),
            ) + history,
        )
        return llmRepository.streamChat(request)
    }
}
