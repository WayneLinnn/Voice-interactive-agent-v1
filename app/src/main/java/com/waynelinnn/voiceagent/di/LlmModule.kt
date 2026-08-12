package com.waynelinnn.voiceagent.di

import com.waynelinnn.voiceagent.domain.llm.LlmClient
import com.waynelinnn.voiceagent.domain.llm.LlmProvider
import com.waynelinnn.voiceagent.llm.RoutingLlmClient
import com.waynelinnn.voiceagent.llm.providers.DeepSeekProvider
import com.waynelinnn.voiceagent.llm.providers.OpenAiProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {
    @Binds
    @Singleton
    abstract fun bindLlmClient(impl: RoutingLlmClient): LlmClient

    @Binds
    @IntoSet
    abstract fun bindOpenAiProvider(impl: OpenAiProvider): LlmProvider

    @Binds
    @IntoSet
    abstract fun bindDeepSeekProvider(impl: DeepSeekProvider): LlmProvider
}
