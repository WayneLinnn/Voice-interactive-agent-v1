package com.waynelinnn.voiceagent.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TtsEvent
import com.waynelinnn.voiceagent.domain.tts.TextToSpeechClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android framework TTS (device/system voices).
 * Works offline when the OEM language pack is installed.
 */
@Singleton
class AndroidSystemTextToSpeechClient @Inject constructor(
    @ApplicationContext context: Context,
) : TextToSpeechClient {

    override val engineName: String = "android-system-tts"

    private val ready = AtomicBoolean(false)
    private val _events = MutableSharedFlow<TtsEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<TtsEvent> = _events.asSharedFlow()

    private var tts: TextToSpeech? = null

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _events.tryEmit(TtsEvent.Started)
        }

        override fun onDone(utteranceId: String?) {
            _events.tryEmit(TtsEvent.Completed)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _events.tryEmit(TtsEvent.Error("System TTS utterance error"))
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            _events.tryEmit(TtsEvent.Error("System TTS error code=$errorCode"))
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(utteranceListener)
                ready.set(true)
                Log.i(TAG, "System TTS ready")
            } else {
                ready.set(false)
                _events.tryEmit(TtsEvent.Error("System TTS init failed: status=$status"))
                Log.e(TAG, "System TTS init failed: $status")
            }
        }
    }

    override fun speak(text: String, language: SpeechLanguage): Boolean {
        val engine = tts
        val trimmed = text.trim()
        if (trimmed.isEmpty() || engine == null) return false
        if (!ready.get()) {
            _events.tryEmit(TtsEvent.Error("System TTS not ready yet"))
            return false
        }
        val locale = localeFor(language, trimmed)
        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA ||
            langResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            engine.language = Locale.getDefault()
            Log.w(TAG, "Locale $locale missing; using default ${Locale.getDefault()}")
        }
        engine.stop()
        val utteranceId = UUID.randomUUID().toString()
        val result = engine.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            _events.tryEmit(TtsEvent.Error("speak() failed: $result"))
            return false
        }
        return true
    }

    override fun stop() {
        runCatching { tts?.stop() }
    }

    override fun shutdown() {
        ready.set(false)
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun localeFor(language: SpeechLanguage, text: String): Locale = when (language) {
        SpeechLanguage.Chinese -> Locale.SIMPLIFIED_CHINESE
        SpeechLanguage.English -> Locale.US
        SpeechLanguage.Auto -> detectLocale(text)
    }

    private fun detectLocale(text: String): Locale {
        val hasCjk = text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        return if (hasCjk) Locale.SIMPLIFIED_CHINESE else Locale.US
    }

    companion object {
        private const val TAG = "AndroidSystemTts"
    }
}
