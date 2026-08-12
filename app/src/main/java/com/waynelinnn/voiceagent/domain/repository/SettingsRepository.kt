package com.waynelinnn.voiceagent.domain.repository

import com.waynelinnn.voiceagent.domain.model.AppSettings
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setDefaultModelId(modelId: String)
    suspend fun setSpeechLanguage(language: SpeechLanguage)
    suspend fun setVoiceId(voiceId: String)
    suspend fun setSpeechRate(rate: Float)
    suspend fun setWakeWordEnabled(enabled: Boolean)
}
