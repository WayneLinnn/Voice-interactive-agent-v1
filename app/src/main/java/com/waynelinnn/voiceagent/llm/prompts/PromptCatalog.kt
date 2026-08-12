package com.waynelinnn.voiceagent.llm.prompts

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads text prompts from `assets/llm/prompts/` (course-style prompt files).
 * Cached after first read to avoid repeated asset I/O on every turn.
 */
@Singleton
class PromptCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var voiceAssistantCached: String? = null

    fun voiceAssistantSystemPrompt(): String {
        voiceAssistantCached?.let { return it }
        return readAsset("llm/prompts/voice_assistant.txt").also { voiceAssistantCached = it }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).use { input ->
            BufferedReader(input.reader(StandardCharsets.UTF_8)).readText().trim()
        }
}
