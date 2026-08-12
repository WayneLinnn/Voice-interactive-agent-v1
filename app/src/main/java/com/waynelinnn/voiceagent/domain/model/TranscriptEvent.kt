package com.waynelinnn.voiceagent.domain.model

sealed interface TranscriptEvent {
    data class Partial(
        val text: String,
        val languageHint: String? = null,
    ) : TranscriptEvent

    data class Final(
        val text: String,
        val languageHint: String? = null,
    ) : TranscriptEvent

    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : TranscriptEvent
}
