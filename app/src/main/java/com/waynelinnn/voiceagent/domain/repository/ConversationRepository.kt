package com.waynelinnn.voiceagent.domain.repository

import com.waynelinnn.voiceagent.domain.model.ChatMessage
import com.waynelinnn.voiceagent.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeSessions(): Flow<List<ChatSession>>
    fun observeMessages(sessionId: Long): Flow<List<ChatMessage>>
    suspend fun createSession(title: String, modelId: String): Long
    suspend fun appendMessage(sessionId: Long, message: ChatMessage): Long
    suspend fun updateSessionTitle(sessionId: Long, title: String)
    suspend fun getSession(sessionId: Long): ChatSession?
    suspend fun deleteSession(sessionId: Long)
    suspend fun latestSessionOrNull(): ChatSession?
    /** Oldest → newest, up to [limit] most recent messages. */
    suspend fun recentMessages(sessionId: Long, limit: Int): List<ChatMessage>
}
