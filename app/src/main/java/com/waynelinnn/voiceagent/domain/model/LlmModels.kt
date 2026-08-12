package com.waynelinnn.voiceagent.domain.model

data class LlmChatMessage(
    val role: MessageRole,
    val content: String,
)

data class LlmChatRequest(
    val modelId: String,
    val messages: List<LlmChatMessage>,
)

sealed interface LlmStreamEvent {
    data class Token(val text: String) : LlmStreamEvent
    data object Completed : LlmStreamEvent
    data class Error(val message: String, val cause: Throwable? = null) : LlmStreamEvent
}
