package com.waynelinnn.voiceagent.domain.conversation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which Room session the next voice start should resume (set from History UI). */
@Singleton
class ActiveConversationStore @Inject constructor() {
    private val _preferredSessionId = MutableStateFlow<Long?>(null)
    val preferredSessionId: StateFlow<Long?> = _preferredSessionId.asStateFlow()

    fun setPreferredSessionId(sessionId: Long?) {
        _preferredSessionId.value = sessionId
    }

    fun clearIf(sessionId: Long) {
        if (_preferredSessionId.value == sessionId) {
            _preferredSessionId.value = null
        }
    }
}
