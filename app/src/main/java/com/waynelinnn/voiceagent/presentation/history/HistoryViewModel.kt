package com.waynelinnn.voiceagent.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waynelinnn.voiceagent.domain.conversation.ActiveConversationStore
import com.waynelinnn.voiceagent.domain.model.ChatSession
import com.waynelinnn.voiceagent.domain.repository.ConversationRepository
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<ChatSession> = emptyList(),
    val preferredSessionId: Long? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
    private val activeConversationStore: ActiveConversationStore,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        conversationRepository.observeSessions(),
        activeConversationStore.preferredSessionId,
    ) { sessions, preferred ->
        HistoryUiState(sessions = sessions, preferredSessionId = preferred)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistoryUiState(),
    )

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            activeConversationStore.clearIf(sessionId)
            conversationRepository.deleteSession(sessionId)
        }
    }

    fun selectSession(sessionId: Long) {
        activeConversationStore.setPreferredSessionId(sessionId)
    }

    fun createNewSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val modelId = runCatching {
                settingsRepository.settings.first().defaultModelId
            }.getOrDefault("gpt-4o-mini")
            val id = conversationRepository.createSession(
                title = "New chat",
                modelId = modelId,
            )
            activeConversationStore.setPreferredSessionId(id)
            onCreated(id)
        }
    }
}
