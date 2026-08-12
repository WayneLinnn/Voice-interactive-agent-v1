package com.waynelinnn.voiceagent.data.remote.api

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit contract for OpenAI-compatible chat completions.
 * Streaming uses OkHttp SSE directly; this covers non-stream fallback later.
 */
interface LlmApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body body: ChatCompletionRequestDto,
    ): ChatCompletionResponseDto
}

data class ChatCompletionRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false,
)

data class ChatMessageDto(
    val role: String,
    val content: String,
)

data class ChatCompletionResponseDto(
    val id: String? = null,
    val choices: List<ChatChoiceDto> = emptyList(),
)

data class ChatChoiceDto(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    val delta: ChatDeltaDto? = null,
    @Json(name = "finish_reason") val finishReason: String? = null,
)

data class ChatDeltaDto(
    val role: String? = null,
    val content: String? = null,
)

data class ChatCompletionChunkDto(
    val id: String? = null,
    val choices: List<ChatChoiceDto> = emptyList(),
)
