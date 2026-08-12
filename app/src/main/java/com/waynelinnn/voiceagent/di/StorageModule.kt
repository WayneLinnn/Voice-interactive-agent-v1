package com.waynelinnn.voiceagent.di

import android.content.Context
import androidx.room.Room
import com.waynelinnn.voiceagent.data.local.db.ChatDao
import com.waynelinnn.voiceagent.data.local.db.VoiceAgentDatabase
import com.waynelinnn.voiceagent.data.repository.ConversationRepositoryImpl
import com.waynelinnn.voiceagent.data.repository.SettingsRepositoryImpl
import com.waynelinnn.voiceagent.domain.repository.ConversationRepository
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): VoiceAgentDatabase = Room.databaseBuilder(
        context,
        VoiceAgentDatabase::class.java,
        "voice_agent.db",
    ).build()

    @Provides
    fun provideChatDao(database: VoiceAgentDatabase): ChatDao = database.chatDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        impl: ConversationRepositoryImpl,
    ): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository
}
