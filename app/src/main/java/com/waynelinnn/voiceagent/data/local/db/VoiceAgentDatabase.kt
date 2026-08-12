package com.waynelinnn.voiceagent.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VoiceAgentDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
