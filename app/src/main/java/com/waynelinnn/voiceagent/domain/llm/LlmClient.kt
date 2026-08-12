package com.waynelinnn.voiceagent.domain.llm

import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * Provider-facing LLM streaming client.
 * Swap implementations (OpenAI, Azure, …) without changing UI / repository callers.
 */
interface LlmClient {
    val providerId: String

    fun streamChat(request: LlmChatRequest): Flow<LlmStreamEvent>
}
