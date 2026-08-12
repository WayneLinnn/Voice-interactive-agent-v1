package com.waynelinnn.voiceagent.domain.model

enum class MessageRole {
    User,
    Assistant,
    System,
}

data class ChatMessage(
    val id: Long = 0L,
    val sessionId: Long,
    val role: MessageRole,
    val content: String,
    val createdAtEpochMs: Long,
)

data class ChatSession(
    val id: Long = 0L,
    val title: String,
    val modelId: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
