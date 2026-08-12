package com.waynelinnn.voiceagent.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.waynelinnn.voiceagent.domain.model.AppSettings
import com.waynelinnn.voiceagent.domain.model.SpeechLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "voice_agent_settings",
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            defaultModelId = prefs[Keys.DEFAULT_MODEL_ID] ?: AppSettings.DEFAULT_MODEL_ID,
            speechLanguage = prefs[Keys.SPEECH_LANGUAGE]
                ?.let { runCatching { SpeechLanguage.valueOf(it) }.getOrNull() }
                ?: SpeechLanguage.Auto,
            voiceId = prefs[Keys.VOICE_ID] ?: AppSettings.DEFAULT_VOICE_ID,
            speechRate = prefs[Keys.SPEECH_RATE] ?: AppSettings.DEFAULT_SPEECH_RATE,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
        )
    }

    suspend fun setDefaultModelId(modelId: String) {
        dataStore.edit { it[Keys.DEFAULT_MODEL_ID] = modelId }
    }

    suspend fun setSpeechLanguage(language: SpeechLanguage) {
        dataStore.edit { it[Keys.SPEECH_LANGUAGE] = language.name }
    }

    suspend fun setVoiceId(voiceId: String) {
        dataStore.edit { it[Keys.VOICE_ID] = voiceId }
    }

    suspend fun setSpeechRate(rate: Float) {
        dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    private object Keys {
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val SPEECH_LANGUAGE = stringPreferencesKey("speech_language")
        val VOICE_ID = stringPreferencesKey("voice_id")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    }
}
