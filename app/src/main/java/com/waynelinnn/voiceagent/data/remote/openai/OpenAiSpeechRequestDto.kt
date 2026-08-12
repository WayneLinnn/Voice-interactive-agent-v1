package com.waynelinnn.voiceagent.data.remote.openai

import com.squareup.moshi.Json

data class OpenAiSpeechRequestDto(
    val model: String,
    val input: String,
    val voice: String,
    @Json(name = "response_format") val responseFormat: String = "mp3",
    val speed: Double = 1.0,
)
