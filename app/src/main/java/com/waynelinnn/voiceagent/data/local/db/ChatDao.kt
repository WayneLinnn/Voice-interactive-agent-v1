package com.waynelinnn.voiceagent.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtEpochMs DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtEpochMs ASC")
    fun observeMessages(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_sessions SET updatedAtEpochMs = :updatedAtEpochMs WHERE id = :sessionId")
    suspend fun touchSession(sessionId: Long, updatedAtEpochMs: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun latestSession(): ChatSessionEntity?

    @Query(
        """
        SELECT * FROM chat_messages
        WHERE sessionId = :sessionId
        ORDER BY createdAtEpochMs DESC
        LIMIT :limit
        """,
    )
    suspend fun recentMessagesDesc(sessionId: Long, limit: Int): List<ChatMessageEntity>

    @Transaction
    suspend fun appendMessageAndTouch(message: ChatMessageEntity): Long {
        val messageId = insertMessage(message)
        touchSession(message.sessionId, message.createdAtEpochMs)
        return messageId
    }
}
