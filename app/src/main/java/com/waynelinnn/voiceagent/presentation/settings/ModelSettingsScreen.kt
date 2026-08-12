package com.waynelinnn.voiceagent.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TtsVoiceOption
import com.waynelinnn.voiceagent.presentation.theme.QuantisBlack
import com.waynelinnn.voiceagent.presentation.theme.QuantisMagenta
import com.waynelinnn.voiceagent.presentation.theme.QuantisMuted
import com.waynelinnn.voiceagent.presentation.theme.QuantisText

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
        onSelectLanguage = viewModel::selectSpeechLanguage,
        onSelectVoice = viewModel::selectVoice,
        onSelectSpeechRate = viewModel::selectSpeechRate,
        onWakeWordChanged = viewModel::setWakeWordEnabled,
        onTestGpt4oMini = viewModel::runGpt4oMiniTest,
    )
}

@Composable
fun ModelSettingsScreen(
    uiState: ModelSettingsUiState,
    onBack: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectLanguage: (SpeechLanguage) -> Unit,
    onSelectVoice: (String) -> Unit,
    onSelectSpeechRate: (Float) -> Unit,
    onWakeWordChanged: (Boolean) -> Unit,
    onTestGpt4oMini: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuantisBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(onClick = onBack) {
            Text(text = stringResource(R.string.model_settings_back), color = QuantisMagenta)
        }
        Text(
            text = stringResource(R.string.model_settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = QuantisText,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (uiState.hasApiKey) {
                stringResource(R.string.model_api_key_ready_dev)
            } else {
                stringResource(R.string.model_api_key_missing_dev)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = QuantisMuted,
        )

        SettingsSection(title = stringResource(R.string.settings_section_language)) {
            SpeechLanguage.entries.forEach { language ->
                ChoiceRow(
                    title = languageLabel(language),
                    selected = uiState.speechLanguage == language,
                    onSelect = { onSelectLanguage(language) },
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_voice)) {
            TtsVoiceOption.entries.forEach { voice ->
                ChoiceRow(
                    title = voice.label,
                    subtitle = voice.id,
                    selected = uiState.voiceId == voice.id,
                    onSelect = { onSelectVoice(voice.id) },
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_rate)) {
            listOf(0.75f, 1.0f, 1.25f).forEach { rate ->
                ChoiceRow(
                    title = rateLabel(rate),
                    selected = uiState.speechRate == rate,
                    onSelect = { onSelectSpeechRate(rate) },
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_section_wake)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_wake_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = QuantisText,
                    )
                    Text(
                        text = stringResource(R.string.settings_wake_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = QuantisMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = uiState.wakeWordEnabled,
                    onCheckedChange = onWakeWordChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = QuantisText,
                        checkedTrackColor = QuantisMagenta,
                        uncheckedThumbColor = QuantisMuted,
                        uncheckedTrackColor = QuantisMuted.copy(alpha = 0.3f),
                    ),
                )
            }
        }

        SettingsSection(title = stringResource(R.string.model_picker_title)) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onTestGpt4oMini,
                enabled = !uiState.testRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantisMagenta,
                    contentColor = QuantisText,
                ),
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
                    color = QuantisMagenta,
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
            Spacer(modifier = Modifier.height(8.dp))
            uiState.models.forEach { choice ->
                ModelRow(
                    choice = choice,
                    selected = choice.option.id == uiState.selectedModelId,
                    onSelect = { onSelectModel(choice.option.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(modifier = Modifier.height(22.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = QuantisText,
    )
    content()
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = QuantisMagenta,
                unselectedColor = QuantisMuted,
            ),
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = QuantisText)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = QuantisMuted,
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    choice: ModelChoice,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val model = choice.option
    val enabled = choice.selectable
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
            colors = RadioButtonDefaults.colors(
                selectedColor = QuantisMagenta,
                unselectedColor = QuantisMuted,
            ),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = "${model.displayName} (${model.provider.displayName})",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) QuantisText else QuantisMuted.copy(alpha = 0.55f),
            )
            val reason = when (choice.unavailableReason) {
                ModelUnavailableReason.ComingSoon -> stringResource(R.string.model_coming_soon)
                ModelUnavailableReason.MissingApiKey -> stringResource(R.string.model_missing_api_key)
                null -> null
            }
            val subtitle = buildString {
                append(model.id)
                if (model.tags.isNotEmpty()) {
                    append(" · ")
                    append(model.tags.joinToString())
                }
                if (reason != null) {
                    append(" · ")
                    append(reason)
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = QuantisMuted,
            )
        }
    }
}

@Composable
private fun languageLabel(language: SpeechLanguage): String = when (language) {
    SpeechLanguage.Auto -> stringResource(R.string.settings_language_auto)
    SpeechLanguage.Chinese -> stringResource(R.string.settings_language_zh)
    SpeechLanguage.English -> stringResource(R.string.settings_language_en)
}

@Composable
private fun rateLabel(rate: Float): String = when (rate) {
    0.75f -> stringResource(R.string.settings_rate_slow)
    1.25f -> stringResource(R.string.settings_rate_fast)
    else -> stringResource(R.string.settings_rate_normal)
}
