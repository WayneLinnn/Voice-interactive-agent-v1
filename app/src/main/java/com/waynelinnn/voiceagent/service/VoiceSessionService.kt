package com.waynelinnn.voiceagent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.waynelinnn.voiceagent.MainActivity
import com.waynelinnn.voiceagent.R
import com.waynelinnn.voiceagent.audio.AudioFocusManager
import com.waynelinnn.voiceagent.audio.AudioFocusState
import com.waynelinnn.voiceagent.audio.AudioRouteManager
import com.waynelinnn.voiceagent.audio.AudioRouteSnapshot
import com.waynelinnn.voiceagent.audio.capture.PcmAudioCapture
import com.waynelinnn.voiceagent.audio.vad.EnergyVad
import com.waynelinnn.voiceagent.audio.vad.VadEvent
import com.waynelinnn.voiceagent.domain.conversation.ActiveConversationStore
import com.waynelinnn.voiceagent.domain.conversation.ConversationContextPolicy
import com.waynelinnn.voiceagent.domain.conversation.ReplySentenceSplitter
import com.waynelinnn.voiceagent.domain.model.ChatMessage
import com.waynelinnn.voiceagent.domain.model.ListeningState
import com.waynelinnn.voiceagent.domain.model.LlmChatMessage
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import com.waynelinnn.voiceagent.domain.model.MessageRole
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TranscriptEvent
import com.waynelinnn.voiceagent.domain.model.TtsEvent
import com.waynelinnn.voiceagent.domain.performance.VoiceLatencyTrace
import com.waynelinnn.voiceagent.domain.repository.ConversationRepository
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import com.waynelinnn.voiceagent.domain.stt.SpeechToTextClient
import com.waynelinnn.voiceagent.domain.tts.TextToSpeechClient
import com.waynelinnn.voiceagent.domain.wake.WakeWordMatcher
import com.waynelinnn.voiceagent.services.ChatService
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.os.SystemClock

/**
 * Foreground voice session service.
 * Optional soft wake → STT → LLM (6-turn sliding context) → OpenAI TTS,
 * with barge-in + Room persistence.
 */
@AndroidEntryPoint
class VoiceSessionService : Service() {

    @Inject lateinit var speechToTextClient: SpeechToTextClient
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var chatService: ChatService
    @Inject lateinit var textToSpeechClient: TextToSpeechClient
    @Inject lateinit var conversationRepository: ConversationRepository
    @Inject lateinit var activeConversationStore: ActiveConversationStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audioFocusManager: AudioFocusManager
    private lateinit var audioRouteManager: AudioRouteManager

    private var audioCapture: PcmAudioCapture? = null
    private var energyVad: EnergyVad? = null
    private var capturePaused = false
    /** When false, only VAD runs (Speaking barge-in) — saves STT CPU/battery. */
    @Volatile private var feedStt = true
    private var transcriptJob: Job? = null
    private var llmJob: Job? = null
    private var activeSessionId: Long? = null
    private var pendingSessionId: Long? = null
    private var forceNewSession = false
    private var wakeArmed = false
    private val bargeInRequested = AtomicBoolean(false)
    private val chatHistory = mutableListOf<LlmChatMessage>()
    private var titledSession = false
    private var lastNotificationAtMs = 0L

    override fun onCreate() {
        super.onCreate()
        audioFocusManager = AudioFocusManager(this)
        audioRouteManager = AudioRouteManager(this)
        serviceScope.launch {
            audioFocusManager.state.collectLatest { focusState ->
                _focusState.value = focusState
                onFocusChanged(focusState)
                if (_isRunning.value) {
                    updateNotification()
                }
            }
        }
        serviceScope.launch {
            audioRouteManager.snapshot.collectLatest { routeSnapshot ->
                _routeSnapshot.value = routeSnapshot
                if (_isRunning.value) {
                    updateNotification()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSession()
                return START_NOT_STICKY
            }
            else -> {
                pendingSessionId = intent
                    ?.getLongExtra(EXTRA_SESSION_ID, -1L)
                    ?.takeIf { it > 0L }
                forceNewSession = intent?.getBooleanExtra(EXTRA_FORCE_NEW, false) == true
                startSession()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tearDownCapture()
        serviceScope.cancel()
        audioRouteManager.stop()
        audioFocusManager.abandonFocus()
        resetUiState()
        super.onDestroy()
    }

    private fun startSession() {
        ensureNotificationChannel()
        audioFocusManager.requestFocus()
        audioRouteManager.start()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
        _isRunning.value = true
        _focusState.value = audioFocusManager.state.value
        _routeSnapshot.value = audioRouteManager.snapshot.value
        serviceScope.launch {
            ensureActiveConversation()
            if (!_isRunning.value) return@launch
            startCapturePipeline()
            if (audioFocusManager.shouldPausePipelines()) {
                pauseCapture("audio focus not held")
            }
        }
    }

    private fun stopSession() {
        tearDownCapture()
        audioRouteManager.stop()
        audioFocusManager.abandonFocus()
        activeSessionId = null
        pendingSessionId = null
        forceNewSession = false
        wakeArmed = false
        titledSession = false
        bargeInRequested.set(false)
        resetUiState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun ensureActiveConversation() {
        val modelId = runCatching {
            settingsRepository.settings.first().defaultModelId
        }.getOrDefault("gpt-4o-mini")
        val preferred = pendingSessionId
            ?: activeConversationStore.preferredSessionId.value
        pendingSessionId = null

        val resumeId = when {
            forceNewSession -> {
                forceNewSession = false
                null
            }
            preferred != null -> {
                val exists = conversationRepository.getSession(preferred)
                if (exists != null) preferred else null
            }
            else -> conversationRepository.latestSessionOrNull()?.id
        }

        if (resumeId != null) {
            activeSessionId = resumeId
            activeConversationStore.setPreferredSessionId(resumeId)
            val session = conversationRepository.getSession(resumeId)
            titledSession = session?.title
                ?.let { it.isNotBlank() && it != DEFAULT_SESSION_TITLE && it != NEW_SESSION_TITLE }
                ?: false
            val loaded = conversationRepository.recentMessages(
                resumeId,
                ConversationContextPolicy.MAX_MESSAGES,
            )
            chatHistory.clear()
            chatHistory.addAll(
                loaded
                    .filter { it.role == MessageRole.User || it.role == MessageRole.Assistant }
                    .map { LlmChatMessage(role = it.role, content = it.content) },
            )
            applyContextTrim()
            Log.i(TAG, "Resumed session=$resumeId contextMsgs=${chatHistory.size}")
        } else {
            activeSessionId = conversationRepository.createSession(
                title = DEFAULT_SESSION_TITLE,
                modelId = modelId,
            )
            activeConversationStore.setPreferredSessionId(activeSessionId)
            titledSession = false
            chatHistory.clear()
            Log.i(TAG, "Created session=$activeSessionId")
        }
    }

    private fun persistMessage(role: MessageRole, content: String) {
        val sessionId = activeSessionId ?: return
        val text = content.trim()
        if (text.isEmpty()) return
        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                conversationRepository.appendMessage(
                    sessionId,
                    ChatMessage(
                        sessionId = sessionId,
                        role = role,
                        content = text,
                        createdAtEpochMs = System.currentTimeMillis(),
                    ),
                )
                if (role == MessageRole.User && !titledSession) {
                    val title = text.take(28).trim()
                    if (title.isNotEmpty()) {
                        conversationRepository.updateSessionTitle(sessionId, title)
                        titledSession = true
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to persist $role message", error)
            }
        }
    }

    private fun resetUiState() {
        _isRunning.value = false
        _focusState.value = AudioFocusState.Idle
        _routeSnapshot.value = AudioRouteSnapshot()
        _listeningState.value = ListeningState.Idle
        _lastVadEvent.value = null
        _partialTranscript.value = ""
        _finalTranscript.value = ""
        _transcriptHistory.value = emptyList()
        _assistantReply.value = ""
        _assistantError.value = null
        _sttEngineName.value = ""
        chatHistory.clear()
    }

    private fun startCapturePipeline() {
        tearDownCapture()
        capturePaused = false
        _partialTranscript.value = ""
        _finalTranscript.value = ""
        _assistantReply.value = ""
        _assistantError.value = null
        _sttEngineName.value = speechToTextClient.engineName
        // Keep chatHistory — loaded/trimmed by ensureActiveConversation().

        transcriptJob = serviceScope.launch {
            speechToTextClient.transcripts.collectLatest { event ->
                when (event) {
                    is TranscriptEvent.Partial -> {
                        _partialTranscript.value = event.text
                        updateNotification()
                    }
                    is TranscriptEvent.Final -> {
                        _finalTranscript.value = event.text
                        _partialTranscript.value = ""
                        if (event.text.isNotBlank()) {
                            onFinalTranscript(event.text.trim())
                        } else if (
                            _listeningState.value != ListeningState.Paused &&
                            _listeningState.value != ListeningState.Error &&
                            _listeningState.value != ListeningState.Thinking &&
                            _isRunning.value
                        ) {
                            _listeningState.value = if (wakeArmed) {
                                ListeningState.WakeListening
                            } else {
                                ListeningState.Listening
                            }
                            updateNotification()
                        }
                    }
                    is TranscriptEvent.Error -> {
                        Log.e(TAG, "STT error: ${event.message}", event.cause)
                        _listeningState.value = ListeningState.Error
                        updateNotification()
                    }
                }
            }
        }

        serviceScope.launch {
            val language = runCatching {
                settingsRepository.settings.first().speechLanguage
            }.getOrDefault(SpeechLanguage.Auto)
            speechToTextClient.start(language)
        }

        val vad = EnergyVad { event ->
            when (event) {
                VadEvent.SpeechStarted -> {
                    if (_listeningState.value == ListeningState.Speaking) {
                        // Barge-in: resume STT buffering for the new utterance.
                        feedStt = true
                    }
                    speechToTextClient.notifySpeechStarted()
                }
                VadEvent.SpeechEnded -> speechToTextClient.notifySpeechEnded()
            }
            serviceScope.launch {
                val state = _listeningState.value
                // Barge-in while speaking: stop TTS and resume turn-taking on the new utterance.
                if (state == ListeningState.Speaking) {
                    if (event == VadEvent.SpeechStarted) {
                        Log.i(TAG, "Barge-in: stop TTS, keep capturing user speech")
                        bargeInRequested.set(true)
                        textToSpeechClient.stop()
                        llmJob?.cancel()
                        _lastVadEvent.value = event
                        _listeningState.value = ListeningState.SpeechDetected
                        updateNotification()
                    }
                    return@launch
                }
                if (state == ListeningState.Thinking) {
                    return@launch
                }
                _lastVadEvent.value = event
                if (event == VadEvent.SpeechEnded) {
                    VoiceLatencyTrace.mark("vad_speech_ended")
                }
                _listeningState.value = when (event) {
                    VadEvent.SpeechStarted -> ListeningState.SpeechDetected
                    VadEvent.SpeechEnded -> ListeningState.Recognizing
                }
                updateNotification()
            }
        }
        energyVad = vad
        feedStt = true
        audioCapture = PcmAudioCapture(
            onFrame = { frame ->
                if (!capturePaused) {
                    vad.accept(frame)
                    if (feedStt) {
                        speechToTextClient.feedPcm16(frame)
                    }
                }
            },
            onError = { error ->
                Log.e(TAG, "Audio capture error", error)
                serviceScope.launch {
                    _listeningState.value = ListeningState.Error
                    updateNotification()
                }
            },
        ).also { it.start() }
        serviceScope.launch {
            wakeArmed = runCatching {
                settingsRepository.settings.first().wakeWordEnabled
            }.getOrDefault(false)
            _listeningState.value = if (wakeArmed) {
                ListeningState.WakeListening
            } else {
                ListeningState.Listening
            }
            updateNotification()
            Log.i(TAG, "Capture ready wakeArmed=$wakeArmed engine=${textToSpeechClient.engineName}")
        }
        _transcriptHistory.value = emptyList()
    }

    private fun onFinalTranscript(text: String) {
        if (wakeArmed) {
            val wake = WakeWordMatcher.match(text)
            if (wake == null) {
                Log.i(TAG, "Wake armed — ignored: $text")
                _finalTranscript.value = ""
                if (_listeningState.value != ListeningState.Paused &&
                    _listeningState.value != ListeningState.Error &&
                    _isRunning.value
                ) {
                    _listeningState.value = ListeningState.WakeListening
                    updateNotification()
                }
                return
            }
            wakeArmed = false
            Log.i(TAG, "Wake matched command='${wake.command}'")
            _listeningState.value = ListeningState.Listening
            updateNotification()
            val command = wake.command.trim()
            if (command.isEmpty()) {
                _finalTranscript.value = ""
                return
            }
            _finalTranscript.value = command
            _transcriptHistory.value = (_transcriptHistory.value + command).takeLast(8)
            startLlmTurn(command)
            return
        }
        _transcriptHistory.value = (_transcriptHistory.value + text).takeLast(8)
        startLlmTurn(text)
    }

    private fun startLlmTurn(userText: String) {
        llmJob?.cancel()
        textToSpeechClient.stop()
        bargeInRequested.set(false)
        llmJob = serviceScope.launch {
            val turnStartedAt = SystemClock.elapsedRealtime()
            VoiceLatencyTrace.mark("stt_final_to_llm")
            _listeningState.value = ListeningState.Thinking
            _assistantReply.value = ""
            _assistantError.value = null
            capturePaused = true
            feedStt = true
            energyVad?.reset()
            updateNotification()

            chatHistory += LlmChatMessage(role = MessageRole.User, content = userText)
            applyContextTrim()
            persistMessage(MessageRole.User, userText)

            val settings = runCatching { settingsRepository.settings.first() }.getOrNull()
            val modelId = settings?.defaultModelId ?: "gpt-4o-mini"
            val speechLanguage = settings?.speechLanguage ?: SpeechLanguage.Auto

            val replyBuilder = StringBuilder()
            val ttsBuffer = StringBuilder()
            val sentenceChannel = Channel<String>(Channel.UNLIMITED)
            val ttsPump = launch {
                var firstSentence = true
                for (sentence in sentenceChannel) {
                    if (!_isRunning.value || bargeInRequested.get()) break
                    if (firstSentence) {
                        VoiceLatencyTrace.mark("tts_first_sentence", sinceMs = turnStartedAt)
                        firstSentence = false
                    }
                    speakReply(sentence, speechLanguage)
                    if (bargeInRequested.get()) break
                }
            }

            try {
                var firstToken = true
                chatService.streamChat(modelId, chatHistory.toList()).collect { event ->
                    when (event) {
                        is LlmStreamEvent.Token -> {
                            if (firstToken) {
                                VoiceLatencyTrace.mark("llm_first_token", sinceMs = turnStartedAt)
                                firstToken = false
                            }
                            replyBuilder.append(event.text)
                            ttsBuffer.append(event.text)
                            _assistantReply.value = replyBuilder.toString()
                            updateNotificationThrottled()
                            ReplySentenceSplitter.drainComplete(ttsBuffer).forEach { sentence ->
                                sentenceChannel.send(sentence)
                            }
                        }
                        LlmStreamEvent.Completed -> Unit
                        is LlmStreamEvent.Error -> {
                            _assistantError.value = event.message
                            Log.e(TAG, "LLM error: ${event.message}", event.cause)
                        }
                    }
                }
                val remainder = ttsBuffer.toString().trim()
                if (remainder.isNotEmpty() && _assistantError.value == null) {
                    sentenceChannel.send(remainder)
                }
                sentenceChannel.close()
                ttsPump.join()

                val reply = replyBuilder.toString().trim()
                if (reply.isNotEmpty()) {
                    chatHistory += LlmChatMessage(role = MessageRole.Assistant, content = reply)
                    applyContextTrim()
                    persistMessage(MessageRole.Assistant, reply)
                    _assistantReply.value = reply
                    updateNotification()
                }
                VoiceLatencyTrace.mark("llm_turn_done", sinceMs = turnStartedAt)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                sentenceChannel.close()
                ttsPump.cancel()
                textToSpeechClient.stop()
                throw cancelled
            } catch (error: Throwable) {
                sentenceChannel.close()
                ttsPump.cancel()
                _assistantError.value = error.message ?: "LLM failed"
                Log.e(TAG, "LLM turn failed", error)
            } finally {
                if (!sentenceChannel.isClosedForSend) {
                    sentenceChannel.close()
                }
                val barged = bargeInRequested.getAndSet(false)
                finishAssistantTurn(fromBargeIn = barged)
            }
        }
    }

    private suspend fun speakReply(reply: String, language: SpeechLanguage) {
        if (!_isRunning.value || reply.isBlank()) return
        if (bargeInRequested.get()) return
        _listeningState.value = ListeningState.Speaking
        // Mic open for barge-in energy only — skip STT until user actually interrupts.
        capturePaused = false
        feedStt = false
        energyVad?.reset()
        updateNotification()

        coroutineScope {
            val done = async {
                textToSpeechClient.events.first { event ->
                    event is TtsEvent.Completed || event is TtsEvent.Error
                }
            }
            val started = textToSpeechClient.speak(reply, language)
            if (!started) {
                done.cancel()
                Log.w(TAG, "TTS speak() did not start")
                return@coroutineScope
            }
            val terminal = withTimeoutOrNull(120_000L) { done.await() }
            if (terminal == null) {
                Log.w(TAG, "TTS timed out; stopping utterance")
                textToSpeechClient.stop()
                done.cancel()
            } else if (terminal is TtsEvent.Error) {
                Log.e(TAG, "TTS error: ${terminal.message}")
                _assistantError.value = terminal.message
            }
        }
    }

    private fun finishAssistantTurn(fromBargeIn: Boolean = false) {
        if (!_isRunning.value) return
        textToSpeechClient.stop()
        feedStt = true
        if (audioFocusManager.shouldPausePipelines()) {
            pauseCapture("audio focus not held after assistant turn")
            return
        }
        capturePaused = false
        if (fromBargeIn) {
            // User is mid-utterance; do not reset VAD / force Listening.
            if (_listeningState.value != ListeningState.SpeechDetected &&
                _listeningState.value != ListeningState.Recognizing &&
                _listeningState.value != ListeningState.Error
            ) {
                _listeningState.value = ListeningState.SpeechDetected
            }
            updateNotification()
            return
        }
        energyVad?.reset()
        if (_listeningState.value != ListeningState.Error) {
            _listeningState.value = if (wakeArmed) {
                ListeningState.WakeListening
            } else {
                ListeningState.Listening
            }
        }
        updateNotification()
    }

    private fun applyContextTrim() {
        val before = chatHistory.size
        val trimmed = ConversationContextPolicy.trim(chatHistory.toList())
        chatHistory.clear()
        chatHistory.addAll(trimmed)
        if (trimmed.size != before) {
            Log.i(
                TAG,
                "Context trimmed $before → ${chatHistory.size} msgs " +
                    "(maxTurns=${ConversationContextPolicy.MAX_TURNS})",
            )
        }
    }

    private fun tearDownCapture() {
        llmJob?.cancel()
        llmJob = null
        textToSpeechClient.stop()
        transcriptJob?.cancel()
        transcriptJob = null
        speechToTextClient.stop()
        audioCapture?.stop()
        audioCapture = null
        energyVad?.reset()
        energyVad = null
        capturePaused = false
    }

    private fun onFocusChanged(focusState: AudioFocusState) {
        if (!_isRunning.value) return
        when (focusState) {
            AudioFocusState.Held -> {
                val state = _listeningState.value
                if (state != ListeningState.Thinking && state != ListeningState.Speaking) {
                    resumeCapture()
                }
            }
            AudioFocusState.TransientLoss,
            AudioFocusState.Lost,
            AudioFocusState.Idle,
            -> {
                val state = _listeningState.value
                if (state == ListeningState.Speaking || state == ListeningState.Thinking) {
                    textToSpeechClient.stop()
                    capturePaused = true
                } else {
                    pauseCapture("focus=$focusState")
                }
            }
        }
    }

    private fun pauseCapture(reason: String) {
        if (capturePaused && _listeningState.value == ListeningState.Paused) return
        capturePaused = true
        energyVad?.reset()
        if (_listeningState.value != ListeningState.Thinking &&
            _listeningState.value != ListeningState.Speaking
        ) {
            _listeningState.value = ListeningState.Paused
        }
        _lastVadEvent.value = null
        Log.i(TAG, "Capture paused: $reason")
    }

    private fun resumeCapture() {
        if (!_isRunning.value) return
        if (audioCapture == null) {
            startCapturePipeline()
            return
        }
        val state = _listeningState.value
        if (state == ListeningState.Thinking || state == ListeningState.Speaking) return
        if (!capturePaused &&
            (state == ListeningState.Listening || state == ListeningState.WakeListening)
        ) {
            return
        }
        capturePaused = false
        energyVad?.reset()
        _listeningState.value = if (wakeArmed) {
            ListeningState.WakeListening
        } else {
            ListeningState.Listening
        }
        Log.i(TAG, "Capture resumed")
    }

    private fun updateNotificationThrottled(minIntervalMs: Long = 280L) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotificationAtMs < minIntervalMs) return
        lastNotificationAtMs = now
        updateNotification()
    }

    private fun updateNotification() {
        lastNotificationAtMs = SystemClock.elapsedRealtime()
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.voice_session_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.voice_session_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val listeningText = listeningTextFor(_listeningState.value)
        val transcript = _partialTranscript.value.ifBlank { _finalTranscript.value }
        val assistant = _assistantReply.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.voice_session_notification_title))
            .setContentText(listeningText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        buildString {
                            append(listeningText)
                            if (transcript.isNotBlank()) {
                                append('\n')
                                append(transcript)
                            }
                            if (assistant.isNotBlank()) {
                                append('\n')
                                append(assistant.take(160))
                            }
                        },
                    ),
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                getString(R.string.voice_session_stop),
                stopPendingIntent,
            )
            .build()
    }

    private fun listeningTextFor(state: ListeningState): String = when (state) {
        ListeningState.Idle -> getString(R.string.listening_idle)
        ListeningState.WakeListening -> getString(R.string.listening_wake)
        ListeningState.Listening -> getString(R.string.listening_active)
        ListeningState.SpeechDetected -> getString(R.string.listening_speech_detected)
        ListeningState.Recognizing -> getString(R.string.listening_recognizing)
        ListeningState.Thinking -> getString(R.string.listening_thinking)
        ListeningState.Speaking -> getString(R.string.listening_speaking)
        ListeningState.Paused -> getString(R.string.listening_paused)
        ListeningState.Error -> getString(R.string.listening_error)
    }

    companion object {
        private const val TAG = "VoiceSessionService"
        private const val DEFAULT_SESSION_TITLE = "Voice session"
        private const val NEW_SESSION_TITLE = "New chat"
        const val ACTION_START = "com.waynelinnn.voiceagent.action.START_VOICE_SESSION"
        const val ACTION_STOP = "com.waynelinnn.voiceagent.action.STOP_VOICE_SESSION"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_FORCE_NEW = "extra_force_new"
        const val CHANNEL_ID = "voice_session"
        const val NOTIFICATION_ID = 1001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _focusState = MutableStateFlow(AudioFocusState.Idle)
        val focusState: StateFlow<AudioFocusState> = _focusState.asStateFlow()

        private val _routeSnapshot = MutableStateFlow(AudioRouteSnapshot())
        val routeSnapshot: StateFlow<AudioRouteSnapshot> = _routeSnapshot.asStateFlow()

        private val _listeningState = MutableStateFlow(ListeningState.Idle)
        val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

        private val _lastVadEvent = MutableStateFlow<VadEvent?>(null)
        val lastVadEvent: StateFlow<VadEvent?> = _lastVadEvent.asStateFlow()

        private val _partialTranscript = MutableStateFlow("")
        val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

        private val _finalTranscript = MutableStateFlow("")
        val finalTranscript: StateFlow<String> = _finalTranscript.asStateFlow()

        private val _transcriptHistory = MutableStateFlow<List<String>>(emptyList())
        val transcriptHistory: StateFlow<List<String>> = _transcriptHistory.asStateFlow()

        private val _assistantReply = MutableStateFlow("")
        val assistantReply: StateFlow<String> = _assistantReply.asStateFlow()

        private val _assistantError = MutableStateFlow<String?>(null)
        val assistantError: StateFlow<String?> = _assistantError.asStateFlow()

        private val _sttEngineName = MutableStateFlow("")
        val sttEngineName: StateFlow<String> = _sttEngineName.asStateFlow()

        fun startIntent(
            context: Context,
            sessionId: Long? = null,
            forceNew: Boolean = false,
        ): Intent =
            Intent(context, VoiceSessionService::class.java)
                .setAction(ACTION_START)
                .apply {
                    if (sessionId != null && sessionId > 0L) {
                        putExtra(EXTRA_SESSION_ID, sessionId)
                    }
                    if (forceNew) {
                        putExtra(EXTRA_FORCE_NEW, true)
                    }
                }

        fun stopIntent(context: Context): Intent =
            Intent(context, VoiceSessionService::class.java).setAction(ACTION_STOP)

        fun start(
            context: Context,
            sessionId: Long? = null,
            forceNew: Boolean = false,
        ) {
            val intent = startIntent(context, sessionId = sessionId, forceNew = forceNew)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(stopIntent(context))
        }
    }
}
