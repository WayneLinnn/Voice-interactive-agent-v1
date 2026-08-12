package com.waynelinnn.voiceagent.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.squareup.moshi.Moshi
import com.waynelinnn.voiceagent.data.remote.ApiKeyProvider
import com.waynelinnn.voiceagent.data.remote.NetworkConfig
import com.waynelinnn.voiceagent.data.remote.openai.OpenAiSpeechRequestDto
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TtsEvent
import com.waynelinnn.voiceagent.domain.model.TtsVoiceOption
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import com.waynelinnn.voiceagent.domain.tts.TextToSpeechClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenAI Audio Speech API (`tts-1`) → MediaPlayer playback.
 * Uses the same `OPENAI_API_KEY` as chat.
 */
@Singleton
class OpenAiTextToSpeechClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val apiKeyProvider: ApiKeyProvider,
    private val settingsRepository: SettingsRepository,
    moshi: Moshi,
) : TextToSpeechClient {

    override val engineName: String = "openai-tts-1"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestAdapter = moshi.adapter(OpenAiSpeechRequestDto::class.java)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _events = MutableSharedFlow<TtsEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<TtsEvent> = _events.asSharedFlow()

    private val generation = AtomicInteger(0)
    private val playing = AtomicBoolean(false)
    private var speakJob: Job? = null
    @Volatile private var mediaPlayer: MediaPlayer? = null
    private val cacheFile: File =
        File(context.cacheDir, "openai_tts_utterance.mp3")

    override fun speak(text: String, language: SpeechLanguage): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _events.tryEmit(
                TtsEvent.Error(
                    "OpenAI API key missing. Set OPENAI_API_KEY in project-root .env and rebuild.",
                ),
            )
            return false
        }

        val gen = generation.incrementAndGet()
        speakJob?.cancel()
        releasePlayer(emitCompleted = false)

        speakJob = scope.launch {
            try {
                val settings = settingsRepository.settings.first()
                val audioBytes = fetchSpeechMp3(trimmed, language, settings.voiceId, settings.speechRate)
                if (gen != generation.get()) return@launch
                playMp3(audioBytes, gen)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                if (gen == generation.get()) {
                    _events.tryEmit(TtsEvent.Completed)
                }
                throw cancelled
            } catch (error: Throwable) {
                if (gen == generation.get()) {
                    Log.e(TAG, "OpenAI TTS failed", error)
                    _events.tryEmit(
                        TtsEvent.Error(error.message ?: "OpenAI TTS failed", error),
                    )
                }
            }
        }
        return true
    }

    override fun stop() {
        generation.incrementAndGet()
        speakJob?.cancel()
        speakJob = null
        releasePlayer(emitCompleted = playing.get())
        playing.set(false)
    }

    override fun shutdown() {
        stop()
        scope.cancel()
        runCatching { if (cacheFile.exists()) cacheFile.delete() }
    }

    private fun fetchSpeechMp3(
        text: String,
        language: SpeechLanguage,
        voiceId: String,
        speechRate: Float,
    ): ByteArray {
        val configuredVoice = voiceId
            .takeIf { id -> TtsVoiceOption.entries.any { it.id == id } }
        val bodyDto = OpenAiSpeechRequestDto(
            model = MODEL,
            input = text.take(MAX_INPUT_CHARS),
            voice = configuredVoice ?: voiceFor(language, text),
            responseFormat = "mp3",
            speed = speechRate.toDouble().coerceIn(0.25, 4.0),
        )
        val json = requestAdapter.toJson(bodyDto)
        val request = Request.Builder()
            .url(NetworkConfig.DEFAULT_BASE_URL + NetworkConfig.SPEECH_PATH)
            .post(json.toRequestBody(jsonMediaType))
            .header("Accept", "audio/mpeg")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string().orEmpty().take(400)
                throw IllegalStateException(
                    "OpenAI TTS HTTP ${response.code}: $errBody",
                )
            }
            return response.body?.bytes()
                ?: throw IllegalStateException("OpenAI TTS empty body")
        }
    }

    private suspend fun playMp3(bytes: ByteArray, gen: Int) {
        withContext(Dispatchers.Main) {
            if (gen != generation.get()) return@withContext
            cacheFile.outputStream().use { it.write(bytes) }
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            player.setDataSource(cacheFile.absolutePath)
            player.setOnCompletionListener {
                if (gen == generation.get()) {
                    playing.set(false)
                    _events.tryEmit(TtsEvent.Completed)
                }
                releasePlayer(emitCompleted = false)
            }
            player.setOnErrorListener { _, what, extra ->
                if (gen == generation.get()) {
                    playing.set(false)
                    _events.tryEmit(TtsEvent.Error("MediaPlayer error what=$what extra=$extra"))
                }
                releasePlayer(emitCompleted = false)
                true
            }
            kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                player.setOnPreparedListener {
                    if (gen != generation.get()) {
                        releasePlayer(emitCompleted = false)
                        if (cont.isActive) cont.resume(Unit)
                        return@setOnPreparedListener
                    }
                    playing.set(true)
                    _events.tryEmit(TtsEvent.Started)
                    player.start()
                    if (cont.isActive) cont.resume(Unit)
                }
                cont.invokeOnCancellation {
                    releasePlayer(emitCompleted = false)
                }
                try {
                    player.prepareAsync()
                } catch (error: Exception) {
                    if (cont.isActive) cont.resumeWithException(error)
                }
            }
        }
    }

    private fun releasePlayer(emitCompleted: Boolean) {
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            runCatching {
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            }
            if (emitCompleted) {
                _events.tryEmit(TtsEvent.Completed)
            }
        } else if (emitCompleted) {
            _events.tryEmit(TtsEvent.Completed)
        }
    }

    private fun voiceFor(language: SpeechLanguage, text: String): String {
        // alloy is solid bilingual; nova slightly warmer for EN-heavy turns.
        val hasCjk = text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        return when (language) {
            SpeechLanguage.Chinese -> "alloy"
            SpeechLanguage.English -> "nova"
            SpeechLanguage.Auto -> if (hasCjk) "alloy" else "nova"
        }
    }

    companion object {
        private const val TAG = "OpenAiTts"
        private const val MODEL = "tts-1"
        private const val MAX_INPUT_CHARS = 4096
    }
}
