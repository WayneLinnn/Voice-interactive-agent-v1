package com.waynelinnn.voiceagent.domain.stt

import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TranscriptEvent
import kotlinx.coroutines.flow.Flow

/**
 * Pluggable speech-to-text engine.
 * Local (Sherpa) and cloud (Azure Speech) implement the same contract.
 */
interface SpeechToTextClient {
    val engineName: String

    /** Hot transcript stream for the active recognition session. */
    val transcripts: Flow<TranscriptEvent>

    fun start(language: SpeechLanguage = SpeechLanguage.Auto)

    /** Feed PCM16 mono frames (typically 16 kHz). */
    fun feedPcm16(frame: ShortArray)

    /** Optional utterance boundary from VAD. */
    fun notifySpeechStarted() {}

    fun notifySpeechEnded() {}

    fun stop()
}
