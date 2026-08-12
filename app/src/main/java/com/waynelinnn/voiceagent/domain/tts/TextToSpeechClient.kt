package com.waynelinnn.voiceagent.domain.tts

import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TtsEvent
import kotlinx.coroutines.flow.Flow

/**
 * Pluggable text-to-speech engine (OpenAI TTS by default; system TTS remains available).
 */
interface TextToSpeechClient {
    val engineName: String

    /** Playback lifecycle for the active speak request. */
    val events: Flow<TtsEvent>

    /**
     * Speak [text]. Cancels any in-progress utterance.
     * @return false if the engine is not ready.
     */
    fun speak(text: String, language: SpeechLanguage = SpeechLanguage.Auto): Boolean

    fun stop()

    fun shutdown()
}
