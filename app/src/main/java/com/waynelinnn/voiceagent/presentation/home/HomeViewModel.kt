package com.waynelinnn.voiceagent.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waynelinnn.voiceagent.data.remote.ApiKeyProvider
import com.waynelinnn.voiceagent.data.stt.SherpaModelStore
import com.waynelinnn.voiceagent.data.stt.SttModelState
import com.waynelinnn.voiceagent.domain.model.AppSettings
import com.waynelinnn.voiceagent.domain.model.LlmModelCatalog
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val title: String = "Quantis",
    val defaultModelId: String = AppSettings.DEFAULT_MODEL_ID,
    val defaultModelLabel: String = "GPT-4o mini",
    val hasOpenAiKey: Boolean = false,
    val speechLanguage: String = AppSettings().speechLanguage.name,
    val voiceId: String = AppSettings.DEFAULT_VOICE_ID,
    val sttModelState: SttModelState = SttModelState.Missing,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val sherpaModelStore: SherpaModelStore,
    apiKeyProvider: ApiKeyProvider,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        sherpaModelStore.state,
    ) { settings, modelState ->
        val option = LlmModelCatalog.find(settings.defaultModelId)
        HomeUiState(
            defaultModelId = settings.defaultModelId,
            defaultModelLabel = option?.displayName ?: settings.defaultModelId,
            hasOpenAiKey = !apiKeyProvider.getApiKey().isNullOrBlank(),
            speechLanguage = settings.speechLanguage.name,
            voiceId = settings.voiceId,
            sttModelState = modelState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            hasOpenAiKey = !apiKeyProvider.getApiKey().isNullOrBlank(),
            sttModelState = sherpaModelStore.state.value,
        ),
    )

    fun downloadSttModel() {
        viewModelScope.launch {
            sherpaModelStore.ensureReady()
        }
    }
}
