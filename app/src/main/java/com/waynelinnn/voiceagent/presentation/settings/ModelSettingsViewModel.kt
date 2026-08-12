package com.waynelinnn.voiceagent.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waynelinnn.voiceagent.data.remote.ApiKeyProvider
import com.waynelinnn.voiceagent.domain.model.AppSettings
import com.waynelinnn.voiceagent.domain.model.LlmChatMessage
import com.waynelinnn.voiceagent.domain.model.LlmChatRequest
import com.waynelinnn.voiceagent.domain.model.LlmModelCatalog
import com.waynelinnn.voiceagent.domain.model.LlmModelOption
import com.waynelinnn.voiceagent.domain.model.LlmStreamEvent
import com.waynelinnn.voiceagent.domain.model.MessageRole
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.model.TtsEvent
import com.waynelinnn.voiceagent.domain.repository.LlmRepository
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import com.waynelinnn.voiceagent.domain.tts.TextToSpeechClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ModelSettingsUiState(
    val models: List<LlmModelOption> = LlmModelCatalog.models,
    val selectedModelId: String = AppSettings.DEFAULT_MODEL_ID,
    val hasApiKey: Boolean = false,
    val testRunning: Boolean = false,
    val testReply: String = "",
    val testError: String? = null,
)

@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val apiKeyProvider: ApiKeyProvider,
    private val llmRepository: LlmRepository,
    private val textToSpeechClient: TextToSpeechClient,
) : ViewModel() {

    private val testState = MutableStateFlow(TestState())
    private var testJob: Job? = null

    private data class TestState(
        val running: Boolean = false,
        val reply: String = "",
        val error: String? = null,
    )

    val uiState: StateFlow<ModelSettingsUiState> = combine(
        settingsRepository.settings,
        testState,
    ) { settings, test ->
        ModelSettingsUiState(
            models = LlmModelCatalog.models,
            selectedModelId = settings.defaultModelId,
            hasApiKey = !apiKeyProvider.getApiKey().isNullOrBlank(),
            testRunning = test.running,
            testReply = test.reply,
            testError = test.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ModelSettingsUiState(
            hasApiKey = !apiKeyProvider.getApiKey().isNullOrBlank(),
        ),
    )

    fun selectModel(modelId: String) {
        if (!LlmModelCatalog.isSelectable(modelId)) return
        viewModelScope.launch {
            settingsRepository.setDefaultModelId(modelId)
        }
    }

    fun runGpt4oMiniTest() {
        if (testState.value.running) return
        if (apiKeyProvider.getApiKey().isNullOrBlank()) {
            testState.value = TestState(
                error = "No API key. Set OPENAI_API_KEY in project-root .env and rebuild.",
            )
            return
        }
        testJob?.cancel()
        textToSpeechClient.stop()
        testState.value = TestState(running = true)
        val modelId = "gpt-4o-mini"
        viewModelScope.launch {
            settingsRepository.setDefaultModelId(modelId)
        }
        testJob = viewModelScope.launch {
            val request = LlmChatRequest(
                modelId = modelId,
                messages = listOf(
                    LlmChatMessage(
                        role = MessageRole.User,
                        content = "Reply with exactly one short bilingual line: 你好 OpenAI TTS. Hello.",
                    ),
                ),
            )
            runCatching {
                llmRepository.streamChat(request).collect { event ->
                    when (event) {
                        is LlmStreamEvent.Token -> {
                            testState.update { it.copy(reply = it.reply + event.text, error = null) }
                        }
                        LlmStreamEvent.Completed -> {
                            testState.update { it.copy(running = false) }
                        }
                        is LlmStreamEvent.Error -> {
                            testState.value = TestState(
                                running = false,
                                reply = testState.value.reply,
                                error = event.message,
                            )
                        }
                    }
                }
            }.onFailure { error ->
                testState.value = TestState(
                    running = false,
                    reply = testState.value.reply,
                    error = error.message ?: "Unknown stream error",
                )
            }
            if (testState.value.running) {
                testState.update { it.copy(running = false) }
            }
            val reply = testState.value.reply.trim()
            if (reply.isNotEmpty() && testState.value.error == null) {
                val waiter = launch {
                    textToSpeechClient.events.first {
                        it is TtsEvent.Completed || it is TtsEvent.Error
                    }
                }
                if (textToSpeechClient.speak(reply, SpeechLanguage.Auto)) {
                    withTimeoutOrNull(60_000L) { waiter.join() }
                } else {
                    waiter.cancel()
                }
            }
        }
    }

    override fun onCleared() {
        textToSpeechClient.stop()
        super.onCleared()
    }
}
