package com.waynelinnn.voiceagent.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waynelinnn.voiceagent.domain.conversation.ActiveConversationStore
import com.waynelinnn.voiceagent.domain.model.ChatMessage
import com.waynelinnn.voiceagent.domain.model.ChatSession
import com.waynelinnn.voiceagent.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConversationDetailUiState(
    val session: ChatSession? = null,
    val messages: List<ChatMessage> = emptyList(),
)

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    conversationRepository: ConversationRepository,
    private val activeConversationStore: ActiveConversationStore,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val uiState: StateFlow<ConversationDetailUiState> = combine(
        conversationRepository.observeSessions(),
        conversationRepository.observeMessages(sessionId),
    ) { sessions, messages ->
        ConversationDetailUiState(
            session = sessions.firstOrNull { it.id == sessionId },
            messages = messages,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConversationDetailUiState(),
    )

    fun markPreferred() {
        activeConversationStore.setPreferredSessionId(sessionId)
    }
}
