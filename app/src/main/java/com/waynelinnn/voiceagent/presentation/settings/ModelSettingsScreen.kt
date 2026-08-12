package com.waynelinnn.voiceagent.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waynelinnn.voiceagent.R
import com.waynelinnn.voiceagent.domain.model.LlmModelOption

@Composable
fun ModelSettingsRoute(
    onBack: () -> Unit,
    viewModel: ModelSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ModelSettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectModel = viewModel::selectModel,
        onTestGpt4oMini = viewModel::runGpt4oMiniTest,
    )
}

@Composable
fun ModelSettingsScreen(
    uiState: ModelSettingsUiState,
    onBack: () -> Unit,
    onSelectModel: (String) -> Unit,
    onTestGpt4oMini: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.model_settings_back))
        }
        Text(
            text = stringResource(R.string.model_settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (uiState.hasApiKey) {
                stringResource(R.string.model_api_key_ready_dev)
            } else {
                stringResource(R.string.model_api_key_missing_dev)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onTestGpt4oMini,
            enabled = !uiState.testRunning,
        ) {
            Text(
                text = if (uiState.testRunning) {
                    stringResource(R.string.model_test_running)
                } else {
                    stringResource(R.string.model_test_button)
                },
            )
        }
        if (uiState.testReply.isNotBlank()) {
            Text(
                text = stringResource(R.string.model_test_reply_label, uiState.testReply),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (uiState.testError != null) {
            Text(
                text = stringResource(R.string.model_test_error_label, uiState.testError.orEmpty()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.model_picker_title),
            style = MaterialTheme.typography.titleMedium,
        )
        uiState.models.forEach { model ->
            ModelRow(
                model = model,
                selected = model.id == uiState.selectedModelId,
                onSelect = { onSelectModel(model.id) },
            )
        }
    }
}

@Composable
private fun ModelRow(
    model: LlmModelOption,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val enabled = model.enabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .then(if (enabled) Modifier.clickable(onClick = onSelect) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected && enabled,
            onClick = if (enabled) onSelect else null,
            enabled = enabled,
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "${model.displayName} (${model.provider.displayName})",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                },
            )
            val subtitle = buildString {
                append(model.id)
                if (model.tags.isNotEmpty()) {
                    append(" · ")
                    append(model.tags.joinToString())
                }
                if (!enabled) append(" · coming soon")
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}
