package com.waynelinnn.voiceagent.data.repository

import com.waynelinnn.voiceagent.data.local.datastore.SettingsDataStore
import com.waynelinnn.voiceagent.domain.model.AppSettings
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import com.waynelinnn.voiceagent.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = settingsDataStore.settings

    override suspend fun setDefaultModelId(modelId: String) {
        settingsDataStore.setDefaultModelId(modelId)
    }

    override suspend fun setSpeechLanguage(language: SpeechLanguage) {
        settingsDataStore.setSpeechLanguage(language)
    }

    override suspend fun setVoiceId(voiceId: String) {
        settingsDataStore.setVoiceId(voiceId)
    }
}
