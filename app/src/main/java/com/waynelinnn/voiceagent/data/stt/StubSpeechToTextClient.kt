package com.waynelinnn.voiceagent.data.stt

import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TranscriptEvent
import com.waynelinnn.voiceagent.domain.stt.SpeechToTextClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Placeholder STT used until Sherpa-ONNX / Azure Speech are wired.
 * Confirms the feed + VAD boundary contract without real recognition.
 */
@Singleton
class StubSpeechToTextClient @Inject constructor() : SpeechToTextClient {
    private val active = AtomicBoolean(false)
    private val framesInUtterance = AtomicInteger(0)
    private var language: SpeechLanguage = SpeechLanguage.Auto

    private val _transcripts = MutableSharedFlow<TranscriptEvent>(
        extraBufferCapacity = 64,
    )

    override val engineName: String = "stub"
    override val transcripts: SharedFlow<TranscriptEvent> = _transcripts.asSharedFlow()

    override fun start(language: SpeechLanguage) {
        this.language = language
        framesInUtterance.set(0)
        active.set(true)
        _transcripts.tryEmit(
            TranscriptEvent.Partial(
                text = "STT stub ready (${language.code})",
                languageHint = language.code,
            ),
        )
    }

    override fun feedPcm16(frame: ShortArray) {
        if (!active.get() || frame.isEmpty()) return
        framesInUtterance.incrementAndGet()
    }

    override fun notifySpeechStarted() {
        if (!active.get()) return
        framesInUtterance.set(0)
        _transcripts.tryEmit(
            TranscriptEvent.Partial(
                text = "…",
                languageHint = language.code,
            ),
        )
    }

    override fun notifySpeechEnded() {
        if (!active.get()) return
        val frames = framesInUtterance.getAndSet(0)
        _transcripts.tryEmit(
            TranscriptEvent.Final(
                text = "[STT stub] heard ~${frames * 32}ms audio — real engine in step 4",
                languageHint = language.code,
            ),
        )
    }

    override fun stop() {
        active.set(false)
        framesInUtterance.set(0)
    }
}
