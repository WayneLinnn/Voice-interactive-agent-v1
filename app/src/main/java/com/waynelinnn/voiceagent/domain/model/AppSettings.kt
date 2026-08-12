package com.waynelinnn.voiceagent.domain.model

enum class SpeechLanguage(val code: String) {
    Auto("auto"),
    Chinese("zh"),
    English("en"),
}

enum class TtsVoiceOption(val id: String, val label: String) {
    Alloy("alloy", "Alloy"),
    Nova("nova", "Nova"),
    Shimmer("shimmer", "Shimmer"),
    Echo("echo", "Echo"),
    Onyx("onyx", "Onyx"),
    Fable("fable", "Fable"),
}

data class AppSettings(
    val defaultModelId: String = DEFAULT_MODEL_ID,
    val speechLanguage: SpeechLanguage = SpeechLanguage.Auto,
    val voiceId: String = DEFAULT_VOICE_ID,
    /** OpenAI TTS speed: 0.75 / 1.0 / 1.25 */
    val speechRate: Float = DEFAULT_SPEECH_RATE,
    /** Soft wake via on-device STT phrase match while the voice session runs. */
    val wakeWordEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_MODEL_ID = "gpt-4o-mini"
        const val DEFAULT_VOICE_ID = "alloy"
        const val DEFAULT_SPEECH_RATE = 1.0f
    }
}
