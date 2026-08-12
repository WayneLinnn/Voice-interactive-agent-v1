package com.waynelinnn.voiceagent.data.local.db

import com.waynelinnn.voiceagent.domain.model.ChatMessage
import com.waynelinnn.voiceagent.domain.model.ChatSession
import com.waynelinnn.voiceagent.domain.model.MessageRole

fun ChatSessionEntity.toDomain(): ChatSession = ChatSession(
    id = id,
    title = title,
    modelId = modelId,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    sessionId = sessionId,
    role = MessageRole.valueOf(role),
    content = content,
    createdAtEpochMs = createdAtEpochMs,
)

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = content,
    createdAtEpochMs = createdAtEpochMs,
)
