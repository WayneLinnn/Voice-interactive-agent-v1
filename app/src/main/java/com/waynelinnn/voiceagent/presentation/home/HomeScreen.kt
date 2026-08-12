package com.waynelinnn.voiceagent.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waynelinnn.voiceagent.R
import com.waynelinnn.voiceagent.audio.AudioFocusState
import com.waynelinnn.voiceagent.audio.vad.VadEvent
import com.waynelinnn.voiceagent.data.stt.SttModelState
import com.waynelinnn.voiceagent.domain.model.ListeningState
import com.waynelinnn.voiceagent.presentation.permission.PermissionGrant
import com.waynelinnn.voiceagent.presentation.permission.rememberVoicePermissionController
import com.waynelinnn.voiceagent.service.VoiceSessionService

@Composable
fun HomeRoute(
    onOpenModelSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberVoicePermissionController()
    val sessionRunning by VoiceSessionService.isRunning.collectAsStateWithLifecycle()
    val focusState by VoiceSessionService.focusState.collectAsStateWithLifecycle()
    val routeSnapshot by VoiceSessionService.routeSnapshot.collectAsStateWithLifecycle()
    val listeningState by VoiceSessionService.listeningState.collectAsStateWithLifecycle()
    val lastVadEvent by VoiceSessionService.lastVadEvent.collectAsStateWithLifecycle()
    val sttEngine by VoiceSessionService.sttEngineName.collectAsStateWithLifecycle()
    val partial by VoiceSessionService.partialTranscript.collectAsStateWithLifecycle()
    val finalText by VoiceSessionService.finalTranscript.collectAsStateWithLifecycle()
    val history by VoiceSessionService.transcriptHistory.collectAsStateWithLifecycle()
    val assistantReply by VoiceSessionService.assistantReply.collectAsStateWithLifecycle()
    val assistantError by VoiceSessionService.assistantError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeScreen(
        uiState = uiState,
        micReady = permissions.snapshot.microphone == PermissionGrant.Granted,
        notificationsReady = permissions.snapshot.notifications == PermissionGrant.Granted,
        bluetoothReady = permissions.bluetoothGranted,
        voiceReady = permissions.snapshot.isVoiceReady,
        sessionRunning = sessionRunning,
        focusState = focusState,
        routeDetail = routeSnapshot.detail,
        listeningState = listeningState,
        lastVadEvent = lastVadEvent,
        sttEngine = sttEngine,
        partialTranscript = partial,
        finalTranscript = finalText,
        transcriptHistory = history,
        assistantReply = assistantReply,
        assistantError = assistantError,
        onEnableVoicePermissions = permissions.requestVoicePermissions,
        onOpenSettings = permissions.openSettings,
        onOpenModelSettings = onOpenModelSettings,
        onDownloadSttModel = viewModel::downloadSttModel,
        onStartSession = {
            permissions.requestHeadsetPermission {
                VoiceSessionService.start(context)
            }
        },
        onStopSession = { VoiceSessionService.stop(context) },
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    micReady: Boolean,
    notificationsReady: Boolean,
    bluetoothReady: Boolean,
    voiceReady: Boolean,
    sessionRunning: Boolean,
    focusState: AudioFocusState,
    routeDetail: String,
    listeningState: ListeningState,
    lastVadEvent: VadEvent?,
    sttEngine: String,
    partialTranscript: String,
    finalTranscript: String,
    transcriptHistory: List<String>,
    assistantReply: String,
    assistantError: String?,
    onEnableVoicePermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onDownloadSttModel: () -> Unit,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
) {
    val modelReady = uiState.sttModelState is SttModelState.Ready
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Local Sherpa SenseVoice",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.home_model_line, uiState.defaultModelLabel, uiState.defaultModelId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = if (uiState.hasOpenAiKey) stringResource(R.string.home_api_key_ready)
            else stringResource(R.string.home_api_key_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = onOpenModelSettings) {
            Text(text = stringResource(R.string.model_settings_open))
        }
        Spacer(modifier = Modifier.height(8.dp))
        StatusLine(
            when (val state = uiState.sttModelState) {
                SttModelState.Missing -> stringResource(R.string.stt_model_missing)
                is SttModelState.Downloading -> stringResource(
                    R.string.stt_model_downloading,
                    (state.progress * 100).toInt(),
                )
                SttModelState.Ready -> stringResource(R.string.stt_model_ready)
                is SttModelState.Failed -> stringResource(R.string.stt_model_failed, state.message)
            },
            emphasize = true,
        )
        if (!modelReady && uiState.sttModelState !is SttModelState.Downloading) {
            OutlinedButton(
                onClick = onDownloadSttModel,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(text = stringResource(R.string.stt_download_model))
            }
        }
        StatusLine(if (micReady) stringResource(R.string.permission_mic_ready) else stringResource(R.string.permission_mic_missing))
        StatusLine(if (sessionRunning) stringResource(R.string.voice_session_running) else stringResource(R.string.voice_session_idle))
        StatusLine(
            when (listeningState) {
                ListeningState.Idle -> stringResource(R.string.listening_idle)
                ListeningState.Listening -> stringResource(R.string.listening_active)
                ListeningState.SpeechDetected -> stringResource(R.string.listening_speech_detected)
                ListeningState.Recognizing -> stringResource(R.string.listening_recognizing)
                ListeningState.Thinking -> stringResource(R.string.listening_thinking)
                ListeningState.Speaking -> stringResource(R.string.listening_speaking)
                ListeningState.Paused -> stringResource(R.string.listening_paused)
                ListeningState.Error -> stringResource(R.string.listening_error)
            },
            emphasize = true,
        )
        StatusLine(
            when (lastVadEvent) {
                VadEvent.SpeechStarted -> stringResource(R.string.vad_speech_started)
                VadEvent.SpeechEnded -> stringResource(R.string.vad_speech_ended)
                null -> stringResource(R.string.vad_none)
            },
            emphasize = true,
        )
        if (sttEngine.isNotBlank()) {
            StatusLine(stringResource(R.string.stt_engine_label, sttEngine), emphasize = true)
        }
        when {
            partialTranscript.isNotBlank() -> StatusLine(stringResource(R.string.stt_partial_label, partialTranscript), emphasize = true)
            finalTranscript.isNotBlank() -> StatusLine(stringResource(R.string.stt_final_label, finalTranscript), emphasize = true)
            sessionRunning -> StatusLine(stringResource(R.string.stt_waiting), muted = true)
        }
        if (transcriptHistory.isNotEmpty()) {
            StatusLine(
                stringResource(R.string.turn_history_label, transcriptHistory.takeLast(3).joinToString(" · ")),
                muted = true,
            )
        }
        when {
            assistantError != null -> StatusLine(
                stringResource(R.string.assistant_error_label, assistantError.orEmpty()),
                emphasize = true,
            )
            assistantReply.isNotBlank() -> StatusLine(
                stringResource(R.string.assistant_reply_label, assistantReply),
                emphasize = true,
            )
            sessionRunning -> StatusLine(stringResource(R.string.assistant_waiting), muted = true)
        }
        StatusLine(
            when (focusState) {
                AudioFocusState.Held -> stringResource(R.string.audio_focus_held)
                AudioFocusState.TransientLoss -> stringResource(R.string.audio_focus_paused)
                AudioFocusState.Lost -> stringResource(R.string.audio_focus_lost)
                AudioFocusState.Idle -> stringResource(R.string.audio_focus_idle)
            },
            muted = true,
        )
        StatusLine(stringResource(R.string.audio_route_label, routeDetail), muted = true)
        Spacer(modifier = Modifier.height(16.dp))
        if (!voiceReady) {
            Button(onClick = onEnableVoicePermissions) {
                Text(text = stringResource(R.string.permission_enable_voice))
            }
            TextButton(onClick = onOpenSettings) {
                Text(text = stringResource(R.string.permission_open_settings))
            }
        } else if (sessionRunning) {
            Button(onClick = onStopSession) {
                Text(text = stringResource(R.string.voice_session_stop))
            }
        } else {
            Button(onClick = onStartSession, enabled = true) {
                Text(text = stringResource(R.string.voice_session_start))
            }
        }
    }
}

@Composable
private fun StatusLine(
    text: String,
    muted: Boolean = false,
    emphasize: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = when {
            emphasize -> MaterialTheme.colorScheme.primary
            muted -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onBackground
        },
        modifier = Modifier.padding(top = 4.dp),
    )
}
