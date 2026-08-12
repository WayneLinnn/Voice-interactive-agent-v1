package com.waynelinnn.voiceagent.di

import com.waynelinnn.voiceagent.data.tts.OpenAiTextToSpeechClient
import com.waynelinnn.voiceagent.domain.tts.TextToSpeechClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {
    @Binds
    @Singleton
    abstract fun bindTextToSpeechClient(
        impl: OpenAiTextToSpeechClient,
    ): TextToSpeechClient
}
