package com.waynelinnn.voiceagent.domain.repository

import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import kotlinx.coroutines.flow.Flow

/**
 * Unified LLM entry. Streaming is the default path for voice replies.
 */
interface LlmRepository {
    fun streamChat(request: LlmChatRequest): Flow<LlmStreamEvent>
}
