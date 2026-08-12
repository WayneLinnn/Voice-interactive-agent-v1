package com.waynelinnn.voiceagent.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waynelinnn.voiceagent.R
import com.waynelinnn.voiceagent.domain.model.ChatMessage
import com.waynelinnn.voiceagent.domain.model.MessageRole
import com.waynelinnn.voiceagent.presentation.permission.rememberVoicePermissionController
import com.waynelinnn.voiceagent.presentation.theme.QuantisBlack
import com.waynelinnn.voiceagent.presentation.theme.QuantisMagenta
import com.waynelinnn.voiceagent.presentation.theme.QuantisMuted
import com.waynelinnn.voiceagent.presentation.theme.QuantisSurface
import com.waynelinnn.voiceagent.presentation.theme.QuantisText
import com.waynelinnn.voiceagent.service.VoiceSessionService

@Composable
fun ConversationDetailRoute(
    onBack: () -> Unit,
    viewModel: ConversationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberVoicePermissionController()
    val context = LocalContext.current
    val sessionId = uiState.session?.id
    ConversationDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onContinueVoice = {
            if (sessionId == null) return@ConversationDetailScreen
            viewModel.markPreferred()
            permissions.requestHeadsetPermission {
                VoiceSessionService.start(context, sessionId = sessionId)
            }
        },
    )
}

@Composable
fun ConversationDetailScreen(
    uiState: ConversationDetailUiState,
    onBack: () -> Unit,
    onContinueVoice: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantisBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.model_settings_back), color = QuantisMagenta)
        }
        Text(
            text = uiState.session?.title?.ifBlank { null }
                ?: stringResource(R.string.history_untitled),
            style = MaterialTheme.typography.headlineMedium,
            color = QuantisText,
        )
        Text(
            text = uiState.session?.modelId.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = QuantisMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (uiState.messages.isEmpty()) {
                        Text(
                            text = stringResource(R.string.history_detail_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = QuantisMuted,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 32.dp),
                        )
                        Text(
                            text = stringResource(R.string.history_empty_cta),
                            style = MaterialTheme.typography.bodyMedium,
                            color = QuantisMuted,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }

        Button(
            onClick = onContinueVoice,
            colors = ButtonDefaults.buttonColors(
                containerColor = QuantisMagenta,
                contentColor = QuantisText,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            Text(text = stringResource(R.string.history_continue_voice))
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.User
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = if (isUser) {
                stringResource(R.string.home_you_label)
            } else {
                stringResource(R.string.home_assistant_label)
            }.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = QuantisMuted,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge,
            color = QuantisText,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isUser) QuantisSurface.copy(alpha = 0.95f) else QuantisSurface,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
