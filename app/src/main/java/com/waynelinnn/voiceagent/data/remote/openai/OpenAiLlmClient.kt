package com.waynelinnn.voiceagent.data.remote.openai

import com.squareup.moshi.Moshi
import com.waynelinnn.voiceagent.data.remote.ApiKeyProvider
import com.waynelinnn.voiceagent.data.remote.NetworkConfig
import com.waynelinnn.voiceagent.data.remote.api.ChatCompletionChunkDto
import com.waynelinnn.voiceagent.data.remote.api.ChatCompletionRequestDto
import com.waynelinnn.voiceagent.data.remote.api.ChatMessageDto
import com.waynelinnn.voiceagent.data.remote.stream.SseStreamClient
import com.waynelinnn.voiceagent.domain.llm.LlmClient
import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmProviderId
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import com.waynelinnn.voiceagent.domain.model.MessageRole
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenAI Chat Completions SSE client (gpt-4o-mini / gpt-4o, …).
 */
@Singleton
class OpenAiLlmClient @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
    private val moshi: Moshi,
    private val sseStreamClient: SseStreamClient,
) : LlmClient {

    override val providerId: String = LlmProviderId.OpenAI.id

    private val requestAdapter = moshi.adapter(ChatCompletionRequestDto::class.java)
    private val chunkAdapter = moshi.adapter(ChatCompletionChunkDto::class.java)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun streamChat(request: LlmChatRequest): Flow<LlmStreamEvent> {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return flow {
                emit(
                    LlmStreamEvent.Error(
                        "OpenAI API key missing. Set OPENAI_API_KEY in project-root .env and rebuild.",
                    ),
                )
            }
        }

        val bodyDto = ChatCompletionRequestDto(
            model = request.modelId,
            messages = request.messages.map { message ->
                ChatMessageDto(
                    role = message.role.toApiRole(),
                    content = message.content,
                )
            },
            stream = true,
        )
        val json = requestAdapter.toJson(bodyDto)
        val httpRequest = Request.Builder()
            .url(NetworkConfig.DEFAULT_BASE_URL + NetworkConfig.CHAT_COMPLETIONS_PATH)
            .header("Accept", "text/event-stream")
            .post(json.toRequestBody(jsonMediaType))
            .build()

        return sseStreamClient.stream(httpRequest) { data ->
            val chunk = chunkAdapter.fromJson(data) ?: return@stream null
            val token = chunk.choices.firstOrNull()?.delta?.content
            when {
                !token.isNullOrEmpty() -> LlmStreamEvent.Token(token)
                chunk.choices.firstOrNull()?.finishReason != null -> LlmStreamEvent.Completed
                else -> null
            }
        }
    }

    private fun MessageRole.toApiRole(): String = when (this) {
        MessageRole.User -> "user"
        MessageRole.Assistant -> "assistant"
        MessageRole.System -> "system"
    }
}
