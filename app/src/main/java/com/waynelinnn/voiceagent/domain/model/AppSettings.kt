package com.waynelinnn.voiceagent.domain.model

enum class SpeechLanguage(val code: String) {
    Auto("auto"),
    Chinese("zh"),
    English("en"),
}

data class AppSettings(
    val defaultModelId: String = DEFAULT_MODEL_ID,
    val speechLanguage: SpeechLanguage = SpeechLanguage.Auto,
    val voiceId: String = DEFAULT_VOICE_ID,
) {
    companion object {
        const val DEFAULT_MODEL_ID = "gpt-4o-mini"
        const val DEFAULT_VOICE_ID = "system_default"
    }
}
