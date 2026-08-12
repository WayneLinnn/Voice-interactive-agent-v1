package com.waynelinnn.voiceagent.data.remote

import com.waynelinnn.voiceagent.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injects Bearer token when the request does not already set Authorization
 * (e.g. OpenAI TTS). Provider transports set their own key and must not be overwritten.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!original.header("Authorization").isNullOrBlank()) {
            return chain.proceed(original)
        }
        val key = apiKeyProvider.getApiKey()
        val request = if (key.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $key")
                .build()
        }
        return chain.proceed(request)
    }
}

fun interface ApiKeyProvider {
    /** Default OpenAI key (TTS / legacy callers). */
    fun getApiKey(): String?

    /** Provider-scoped key: openai / deepseek / … */
    fun getProviderApiKey(providerId: String): String? = getApiKey()

    fun hasProviderApiKey(providerId: String): Boolean =
        !getProviderApiKey(providerId).isNullOrBlank()
}

@Singleton
class BuildConfigApiKeyProvider @Inject constructor() : ApiKeyProvider {
    override fun getApiKey(): String? =
        BuildConfig.OPENAI_API_KEY.takeIf { it.isNotBlank() }

    override fun getProviderApiKey(providerId: String): String? = when (providerId) {
        "openai" -> getApiKey()
        "deepseek" -> BuildConfig.DEEPSEEK_API_KEY.takeIf { it.isNotBlank() }
        else -> null
    }
}
