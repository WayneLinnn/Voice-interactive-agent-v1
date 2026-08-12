package com.waynelinnn.voiceagent.domain.model

sealed interface TtsEvent {
    data object Started : TtsEvent
    data object Completed : TtsEvent
    data class Error(val message: String, val cause: Throwable? = null) : TtsEvent
}
