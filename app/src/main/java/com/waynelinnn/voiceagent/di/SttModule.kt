package com.waynelinnn.voiceagent.di

import com.waynelinnn.voiceagent.data.stt.SherpaSpeechToTextClient
import com.waynelinnn.voiceagent.domain.stt.SpeechToTextClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SttModule {
    @Binds
    @Singleton
    abstract fun bindSpeechToTextClient(
        impl: SherpaSpeechToTextClient,
    ): SpeechToTextClient
}
