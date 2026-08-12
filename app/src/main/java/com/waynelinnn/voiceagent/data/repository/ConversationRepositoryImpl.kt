package com.waynelinnn.voiceagent.data.repository

import com.waynelinnn.voiceagent.data.local.db.ChatDao
import com.waynelinnn.voiceagent.data.local.db.ChatSessionEntity
import com.waynelinnn.voiceagent.data.local.db.toDomain
import com.waynelinnn.voiceagent.data.local.db.toEntity
import com.waynelinnn.voiceagent.domain.model.ChatMessage
import com.waynelinnn.voiceagent.domain.model.ChatSession
import com.waynelinnn.voiceagent.domain.repository.ConversationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
) : ConversationRepository {
    override fun observeSessions(): Flow<List<ChatSession>> =
        chatDao.observeSessions().map { sessions -> sessions.map { it.toDomain() } }

    override fun observeMessages(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.observeMessages(sessionId).map { messages -> messages.map { it.toDomain() } }

    override suspend fun createSession(title: String, modelId: String): Long {
        val now = System.currentTimeMillis()
        return chatDao.insertSession(
            ChatSessionEntity(
                title = title,
                modelId = modelId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
    }

    override suspend fun appendMessage(sessionId: Long, message: ChatMessage): Long =
        chatDao.appendMessageAndTouch(message.copy(sessionId = sessionId).toEntity())

    override suspend fun updateSessionTitle(sessionId: Long, title: String) {
        chatDao.updateSessionTitle(
            sessionId = sessionId,
            title = title,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    override suspend fun getSession(sessionId: Long): ChatSession? =
        chatDao.getSession(sessionId)?.toDomain()

    override suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSession(sessionId)
    }

    override suspend fun latestSessionOrNull(): ChatSession? =
        chatDao.latestSession()?.toDomain()

    override suspend fun recentMessages(sessionId: Long, limit: Int): List<ChatMessage> =
        chatDao.recentMessagesDesc(sessionId, limit)
            .asReversed()
            .map { it.toDomain() }
}
