package com.waynelinnn.voiceagent.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waynelinnn.voiceagent.R
import com.waynelinnn.voiceagent.domain.model.ChatSession
import com.waynelinnn.voiceagent.presentation.theme.QuantisBlack
import com.waynelinnn.voiceagent.presentation.theme.QuantisMagenta
import com.waynelinnn.voiceagent.presentation.theme.QuantisMuted
import com.waynelinnn.voiceagent.presentation.theme.QuantisSurface
import com.waynelinnn.voiceagent.presentation.theme.QuantisText
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSession = { id ->
            viewModel.selectSession(id)
            onOpenSession(id)
        },
        onDeleteSession = viewModel::deleteSession,
        onNewChat = {
            viewModel.createNewSession { id -> onOpenSession(id) }
        },
    )
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onNewChat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantisBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(text = stringResource(R.string.model_settings_back), color = QuantisMagenta)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onNewChat) {
                Text(text = stringResource(R.string.history_new_chat), color = QuantisMagenta)
            }
        }
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            color = QuantisText,
        )
        Text(
            text = stringResource(R.string.history_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = QuantisMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (uiState.sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = QuantisMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onBack) {
                            Text(
                                text = stringResource(R.string.history_empty_cta),
                                color = QuantisMagenta,
                            )
                        }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.sessions, key = { it.id }) { session ->
                    HistoryRow(
                        session = session,
                        preferred = session.id == uiState.preferredSessionId,
                        onOpen = { onOpenSession(session.id) },
                        onDelete = { onDeleteSession(session.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    session: ChatSession,
    preferred: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val time = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(session.updatedAtEpochMs))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(QuantisSurface)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = session.title.ifBlank { stringResource(R.string.history_untitled) },
            style = MaterialTheme.typography.titleMedium,
            color = QuantisText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = buildString {
                append(session.modelId)
                append(" · ")
                append(time)
                if (preferred) {
                    append(" · ")
                    append(stringResource(R.string.history_active))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = QuantisMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.history_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
