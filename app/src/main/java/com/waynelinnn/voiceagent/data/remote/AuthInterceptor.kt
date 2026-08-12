package com.waynelinnn.voiceagent.data.remote

import com.waynelinnn.voiceagent.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects Bearer token from build-time `.env` (`OPENAI_API_KEY`).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = apiKeyProvider.getApiKey()
        val request = if (key.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $key")
                .build()
        }
        return chain.proceed(request)
    }
}

fun interface ApiKeyProvider {
    fun getApiKey(): String?
}

@Singleton
class BuildConfigApiKeyProvider @Inject constructor() : ApiKeyProvider {
    override fun getApiKey(): String? =
        BuildConfig.OPENAI_API_KEY.takeIf { it.isNotBlank() }
}
