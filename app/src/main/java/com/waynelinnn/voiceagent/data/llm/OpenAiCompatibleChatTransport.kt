package com.waynelinnn.voiceagent.data.llm

import com.squareup.moshi.Moshi
import com.waynelinnn.voiceagent.data.remote.api.ChatCompletionChunkDto
import com.waynelinnn.voiceagent.data.remote.api.ChatCompletionRequestDto
import com.waynelinnn.voiceagent.data.remote.api.ChatMessageDto
import com.waynelinnn.voiceagent.data.remote.stream.SseStreamClient
import com.waynelinnn.voiceagent.domain.llm.LlmProvider
import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
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
 * Shared OpenAI-compatible Chat Completions SSE transport.
 * Vendor-specific URL/key/model lists live in provider files — not here.
 */
@Singleton
class OpenAiCompatibleChatTransport @Inject constructor(
    moshi: Moshi,
    private val sseStreamClient: SseStreamClient,
) {
    private val requestAdapter = moshi.adapter(ChatCompletionRequestDto::class.java)
    private val chunkAdapter = moshi.adapter(ChatCompletionChunkDto::class.java)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun streamChat(provider: LlmProvider, request: LlmChatRequest): Flow<LlmStreamEvent> {
        val apiKey = provider.apiKey()
        if (apiKey.isNullOrBlank()) {
            return flow { emit(LlmStreamEvent.Error(provider.missingKeyMessage())) }
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
            .url(provider.baseUrl + provider.chatCompletionsPath)
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer $apiKey")
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
