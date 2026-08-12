package com.waynelinnn.voiceagent.domain.conversation

import com.waynelinnn.voiceagent.domain.model.LlmChatMessage
import com.waynelinnn.voiceagent.domain.model.MessageRole

/**
 * Sliding-window policy for multi-turn LLM context.
 *
 * Rationale (voice assistants):
 * - Turns are short; coherence mostly needs the last few exchanges.
 * - Cost scales with full history each request → diminishing returns after ~5–8 turns.
 * - Common product practice: keep ~5–6 turns; we use **6 turns** (12 messages).
 * - Soft char budget ≈ **2000 tokens** (mixed zh/en ≈ 4 chars/token → 8000 chars).
 */
object ConversationContextPolicy {
    /** One turn = one user message + one assistant message. */
    const val MAX_TURNS: Int = 6
    const val MAX_MESSAGES: Int = MAX_TURNS * 2

    /** Soft cap on history content size sent to the model (excludes system prompt). */
    const val MAX_HISTORY_CHARS: Int = 8_000

    fun trim(messages: List<LlmChatMessage>): List<LlmChatMessage> {
        val dialogue = messages.filter {
            it.role == MessageRole.User || it.role == MessageRole.Assistant
        }
        var window = if (dialogue.size > MAX_MESSAGES) {
            dialogue.takeLast(MAX_MESSAGES)
        } else {
            dialogue
        }
        // Drop oldest messages until under char budget (keep pairs when possible).
        while (window.size > 2 && window.sumOf { it.content.length } > MAX_HISTORY_CHARS) {
            window = window.drop(1)
            if (window.firstOrNull()?.role == MessageRole.Assistant) {
                window = window.drop(1)
            }
        }
        return window
    }
}
