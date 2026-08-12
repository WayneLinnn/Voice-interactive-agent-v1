package com.waynelinnn.voiceagent.data.stt

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.waynelinnn.voiceagent.audio.capture.AudioCaptureConfig
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TranscriptEvent
import com.waynelinnn.voiceagent.domain.stt.SpeechToTextClient
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Local bilingual STT via Sherpa-ONNX SenseVoice (zh/en + more).
 * Uses VAD utterance boundaries: buffer PCM → offline decode on SpeechEnded.
 */
@Singleton
class SherpaSpeechToTextClient @Inject constructor(
    private val modelStore: SherpaModelStore,
) : SpeechToTextClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val active = AtomicBoolean(false)
    private val bufferLock = Any()
    private val pcmBuffer = ArrayList<Short>()
    private val initMutex = Mutex()

    @Volatile private var recognizer: OfflineRecognizer? = null
    @Volatile private var language: SpeechLanguage = SpeechLanguage.Auto

    private val _transcripts = MutableSharedFlow<TranscriptEvent>(extraBufferCapacity = 64)
    override val transcripts: SharedFlow<TranscriptEvent> = _transcripts.asSharedFlow()
    override val engineName: String = "sherpa-sensevoice"

    override fun start(language: SpeechLanguage) {
        this.language = language
        synchronized(bufferLock) { pcmBuffer.clear() }
        active.set(true)
        scope.launch {
            val ready = ensureRecognizer()
            if (!ready) {
                _transcripts.tryEmit(
                    TranscriptEvent.Error("Sherpa model not ready. Check download status."),
                )
                return@launch
            }
            _transcripts.tryEmit(
                TranscriptEvent.Partial(
                    text = "Local STT ready (${language.code})",
                    languageHint = language.code,
                ),
            )
        }
    }

    override fun feedPcm16(frame: ShortArray) {
        if (!active.get() || frame.isEmpty()) return
        synchronized(bufferLock) {
            // Cap ~20s to avoid OOM on long noise bursts.
            val maxSamples = AudioCaptureConfig.SAMPLE_RATE_HZ * 20
            if (pcmBuffer.size < maxSamples) {
                for (sample in frame) {
                    pcmBuffer.add(sample)
                    if (pcmBuffer.size >= maxSamples) break
                }
            }
        }
    }

    override fun notifySpeechStarted() {
        if (!active.get()) return
        synchronized(bufferLock) { pcmBuffer.clear() }
        _transcripts.tryEmit(TranscriptEvent.Partial(text = "…", languageHint = language.code))
    }

    override fun notifySpeechEnded() {
        if (!active.get()) return
        val samples: ShortArray
        synchronized(bufferLock) {
            samples = pcmBuffer.toShortArray()
            pcmBuffer.clear()
        }
        if (samples.isEmpty()) return
        scope.launch {
            decodeUtterance(samples)
        }
    }

    override fun stop() {
        active.set(false)
        synchronized(bufferLock) { pcmBuffer.clear() }
    }

    private suspend fun ensureRecognizer(): Boolean = initMutex.withLock {
        if (recognizer != null) return true
        if (!modelStore.ensureReady()) return false
        return try {
            val lang = when (language) {
                SpeechLanguage.Chinese -> "zh"
                SpeechLanguage.English -> "en"
                SpeechLanguage.Auto -> ""
            }
            val config = OfflineRecognizerConfig(
                modelConfig = OfflineModelConfig(
                    senseVoice = OfflineSenseVoiceModelConfig(
                        model = modelStore.modelFile.absolutePath,
                        language = lang,
                        useInverseTextNormalization = true,
                    ),
                    tokens = modelStore.tokensFile.absolutePath,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu",
                ),
            )
            recognizer = OfflineRecognizer(config = config)
            true
        } catch (error: Exception) {
            Log.e(TAG, "Failed to init OfflineRecognizer", error)
            _transcripts.tryEmit(
                TranscriptEvent.Error("Failed to init Sherpa: ${error.message}", error),
            )
            false
        }
    }

    private suspend fun decodeUtterance(pcm: ShortArray) {
        val ready = ensureRecognizer()
        val engine = recognizer
        if (!ready || engine == null) return
        try {
            val floats = FloatArray(pcm.size) { index -> pcm[index] / 32768.0f }
            val stream = engine.createStream()
            stream.acceptWaveform(floats, AudioCaptureConfig.SAMPLE_RATE_HZ)
            engine.decode(stream)
            val result = engine.getResult(stream)
            stream.release()
            val text = result.text.trim()
            if (text.isNotEmpty()) {
                _transcripts.tryEmit(
                    TranscriptEvent.Final(
                        text = text,
                        languageHint = result.lang.ifBlank { language.code },
                    ),
                )
            } else {
                _transcripts.tryEmit(
                    TranscriptEvent.Final(
                        text = "",
                        languageHint = language.code,
                    ),
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "Decode failed", error)
            _transcripts.tryEmit(
                TranscriptEvent.Error("Sherpa decode failed: ${error.message}", error),
            )
        }
    }

    companion object {
        private const val TAG = "SherpaStt"
    }
}
