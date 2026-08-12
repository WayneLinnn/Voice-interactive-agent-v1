package com.waynelinnn.voiceagent.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.waynelinnn.voiceagent.data.remote.ApiKeyProvider
import com.waynelinnn.voiceagent.data.remote.AuthInterceptor
import com.waynelinnn.voiceagent.data.remote.BuildConfigApiKeyProvider
import com.waynelinnn.voiceagent.data.remote.NetworkConfig
import com.waynelinnn.voiceagent.data.remote.api.LlmApi
import com.waynelinnn.voiceagent.data.remote.openai.OpenAiLlmClient
import com.waynelinnn.voiceagent.data.remote.stream.NoOpWebSocketStreamClient
import com.waynelinnn.voiceagent.data.remote.stream.SseStreamClient
import com.waynelinnn.voiceagent.data.remote.stream.WebSocketStreamClient
import com.waynelinnn.voiceagent.data.repository.LlmRepositoryImpl
import com.waynelinnn.voiceagent.domain.llm.LlmClient
import com.waynelinnn.voiceagent.domain.repository.LlmRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(NetworkConfig.DEFAULT_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideLlmApi(retrofit: Retrofit): LlmApi = retrofit.create(LlmApi::class.java)

    @Provides
    @Singleton
    fun provideSseStreamClient(okHttpClient: OkHttpClient): SseStreamClient =
        SseStreamClient(okHttpClient)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindModule {
    @Binds
    @Singleton
    abstract fun bindApiKeyProvider(
        impl: BuildConfigApiKeyProvider,
    ): ApiKeyProvider

    @Binds
    @Singleton
    abstract fun bindLlmClient(
        impl: OpenAiLlmClient,
    ): LlmClient

    @Binds
    @Singleton
    abstract fun bindWebSocketStreamClient(
        impl: NoOpWebSocketStreamClient,
    ): WebSocketStreamClient

    @Binds
    @Singleton
    abstract fun bindLlmRepository(
        impl: LlmRepositoryImpl,
    ): LlmRepository
}
