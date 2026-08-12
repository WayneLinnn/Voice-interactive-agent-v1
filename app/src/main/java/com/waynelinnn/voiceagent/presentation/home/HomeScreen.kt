package com.waynelinnn.voiceagent.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waynelinnn.voiceagent.R
import com.waynelinnn.voiceagent.data.stt.SttModelState
import com.waynelinnn.voiceagent.domain.model.ListeningState
import com.waynelinnn.voiceagent.presentation.common.FriendlyErrors
import com.waynelinnn.voiceagent.presentation.permission.rememberVoicePermissionController
import com.waynelinnn.voiceagent.presentation.theme.QuantisBlack
import com.waynelinnn.voiceagent.presentation.theme.QuantisBlue
import com.waynelinnn.voiceagent.presentation.theme.QuantisMagenta
import com.waynelinnn.voiceagent.presentation.theme.QuantisMuted
import com.waynelinnn.voiceagent.presentation.theme.QuantisSurface
import com.waynelinnn.voiceagent.presentation.theme.QuantisText
import com.waynelinnn.voiceagent.presentation.theme.QuantisViolet
import com.waynelinnn.voiceagent.presentation.theme.QuantisVoid
import com.waynelinnn.voiceagent.presentation.theme.VoiceAgentTheme
import com.waynelinnn.voiceagent.presentation.theme.quantisHorizontalBrush
import com.waynelinnn.voiceagent.service.VoiceSessionService

@Composable
fun HomeRoute(
    onOpenModelSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberVoicePermissionController()
    val sessionRunning by VoiceSessionService.isRunning.collectAsStateWithLifecycle()
    val listeningState by VoiceSessionService.listeningState.collectAsStateWithLifecycle()
    val partial by VoiceSessionService.partialTranscript.collectAsStateWithLifecycle()
    val finalText by VoiceSessionService.finalTranscript.collectAsStateWithLifecycle()
    val assistantReply by VoiceSessionService.assistantReply.collectAsStateWithLifecycle()
    val assistantError by VoiceSessionService.assistantError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeScreen(
        uiState = uiState,
        voiceReady = permissions.snapshot.isVoiceReady,
        sessionRunning = sessionRunning,
        listeningState = listeningState,
        partialTranscript = partial,
        finalTranscript = finalText,
        assistantReply = assistantReply,
        assistantError = assistantError,
        onEnableVoicePermissions = permissions.requestVoicePermissions,
        onOpenSettings = permissions.openSettings,
        onOpenModelSettings = onOpenModelSettings,
        onOpenHistory = onOpenHistory,
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
    voiceReady: Boolean,
    sessionRunning: Boolean,
    listeningState: ListeningState,
    partialTranscript: String,
    finalTranscript: String,
    assistantReply: String,
    assistantError: String?,
    onEnableVoicePermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModelSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onDownloadSttModel: () -> Unit,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
) {
    val modelReady = uiState.sttModelState is SttModelState.Ready
    val statusText = voiceStatusText(sessionRunning, listeningState)
    val userLine = when {
        partialTranscript.isNotBlank() -> partialTranscript
        finalTranscript.isNotBlank() -> finalTranscript
        else -> ""
    }
    val friendlyError = assistantError?.let { raw ->
        val res = FriendlyErrors.assistantErrorRes(raw)
        if (res != null) stringResource(res) else FriendlyErrors.displayMessage(
            raw,
            stringResource(R.string.error_generic),
        )
    }
    val showApiKeyBanner = modelReady && voiceReady && !uiState.hasOpenAiKey && !sessionRunning

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantisBlack)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        QuantisViolet.copy(alpha = 0.18f),
                        QuantisVoid,
                        QuantisBlue.copy(alpha = 0.12f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.quantis_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = QuantisText,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                )
                TextButton(onClick = onOpenHistory) {
                    Text(
                        text = stringResource(R.string.history_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = QuantisMagenta,
                    )
                }
                TextButton(onClick = onOpenModelSettings) {
                    Text(
                        text = stringResource(R.string.model_settings_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = QuantisMagenta,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = QuantisMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(28.dp))
                VoiceOrb(
                    listeningState = listeningState,
                    sessionRunning = sessionRunning,
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (listeningState == ListeningState.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        QuantisText
                    },
                    textAlign = TextAlign.Center,
                )
                if (sessionRunning && listeningState == ListeningState.Paused) {
                    Text(
                        text = stringResource(R.string.home_error_paused_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuantisMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (sessionRunning && listeningState == ListeningState.Error) {
                    Text(
                        text = stringResource(R.string.home_error_mic_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = QuantisMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                if (userLine.isNotBlank()) {
                    TranscriptBlock(
                        label = stringResource(R.string.home_you_label),
                        body = userLine,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                when {
                    friendlyError != null -> TranscriptBlock(
                        label = stringResource(R.string.home_assistant_label),
                        body = friendlyError,
                        emphasize = true,
                    )
                    assistantReply.isNotBlank() -> TranscriptBlock(
                        label = stringResource(R.string.home_assistant_label),
                        body = assistantReply,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    !modelReady -> ModelSetupPanel(
                        state = uiState.sttModelState,
                        onDownload = onDownloadSttModel,
                    )
                    !voiceReady -> PermissionSetupPanel(
                        onEnable = onEnableVoicePermissions,
                        onOpenSettings = onOpenSettings,
                    )
                    else -> {
                        if (showApiKeyBanner) {
                            InfoBanner(
                                title = stringResource(R.string.home_setup_api_key_title),
                                body = stringResource(R.string.home_setup_api_key_body),
                                actionLabel = stringResource(R.string.model_settings_open),
                                onAction = onOpenModelSettings,
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        if (sessionRunning) {
                            QuantisPrimaryButton(
                                text = stringResource(R.string.voice_session_stop),
                                onClick = onStopSession,
                            )
                        } else {
                            QuantisPrimaryButton(
                                text = stringResource(R.string.voice_session_start),
                                onClick = onStartSession,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSetupPanel(
    state: SttModelState,
    onDownload: () -> Unit,
) {
    when (state) {
        is SttModelState.Downloading -> {
            InfoBanner(
                title = stringResource(
                    R.string.home_setup_model_downloading,
                    (state.progress * 100).toInt(),
                ),
                body = stringResource(R.string.home_setup_model_body),
            )
        }
        is SttModelState.Failed -> {
            InfoBanner(
                title = stringResource(R.string.home_setup_model_failed_title),
                body = stringResource(R.string.home_setup_model_failed_body),
                detail = state.message.take(120),
                actionLabel = stringResource(R.string.home_retry_download),
                onAction = onDownload,
            )
        }
        else -> {
            InfoBanner(
                title = stringResource(R.string.home_setup_model),
                body = stringResource(R.string.home_setup_model_body),
                actionLabel = stringResource(R.string.stt_download_model),
                onAction = onDownload,
            )
        }
    }
}

@Composable
private fun PermissionSetupPanel(
    onEnable: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    InfoBanner(
        title = stringResource(R.string.home_setup_permission),
        body = stringResource(R.string.home_setup_permission_body),
    )
    Spacer(modifier = Modifier.height(12.dp))
    QuantisPrimaryButton(
        text = stringResource(R.string.permission_enable_voice),
        onClick = onEnable,
    )
    TextButton(onClick = onOpenSettings) {
        Text(
            text = stringResource(R.string.permission_open_settings),
            color = QuantisMuted,
        )
    }
}

@Composable
private fun InfoBanner(
    title: String,
    body: String,
    detail: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(QuantisSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = QuantisText,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = QuantisMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun QuantisPrimaryButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(quantisHorizontalBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.4.sp),
            )
        }
    }
}

@Composable
private fun TranscriptBlock(
    label: String,
    body: String,
    emphasize: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp),
            color = QuantisMuted.copy(alpha = 0.8f),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = if (emphasize) {
                MaterialTheme.colorScheme.error
            } else {
                QuantisText.copy(alpha = 0.92f)
            },
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun voiceStatusText(
    sessionRunning: Boolean,
    listeningState: ListeningState,
): String {
    if (!sessionRunning) return stringResource(R.string.home_status_ready)
    return when (listeningState) {
        ListeningState.Idle, ListeningState.Listening -> stringResource(R.string.home_status_listening)
        ListeningState.WakeListening -> stringResource(R.string.home_status_wake)
        ListeningState.SpeechDetected -> stringResource(R.string.home_status_speech)
        ListeningState.Recognizing -> stringResource(R.string.home_status_recognizing)
        ListeningState.Thinking -> stringResource(R.string.home_status_thinking)
        ListeningState.Speaking -> stringResource(R.string.home_status_speaking)
        ListeningState.Paused -> stringResource(R.string.home_status_paused)
        ListeningState.Error -> stringResource(R.string.home_status_error)
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Quantis idle", backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenIdlePreview() {
    VoiceAgentTheme {
        HomeScreen(
            uiState = HomeUiState(title = "Quantis", sttModelState = SttModelState.Ready),
            voiceReady = true,
            sessionRunning = false,
            listeningState = ListeningState.Idle,
            partialTranscript = "",
            finalTranscript = "",
            assistantReply = "",
            assistantError = null,
            onEnableVoicePermissions = {},
            onOpenSettings = {},
            onOpenModelSettings = {},
            onOpenHistory = {},
            onDownloadSttModel = {},
            onStartSession = {},
            onStopSession = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Quantis listening", backgroundColor = 0xFF000000)
@Composable
private fun HomeScreenListeningPreview() {
    VoiceAgentTheme {
        HomeScreen(
            uiState = HomeUiState(title = "Quantis", sttModelState = SttModelState.Ready),
            voiceReady = true,
            sessionRunning = true,
            listeningState = ListeningState.Listening,
            partialTranscript = "你好，今天天气怎么样",
            finalTranscript = "",
            assistantReply = "",
            assistantError = null,
            onEnableVoicePermissions = {},
            onOpenSettings = {},
            onOpenModelSettings = {},
            onOpenHistory = {},
            onDownloadSttModel = {},
            onStartSession = {},
            onStopSession = {},
        )
    }
}
