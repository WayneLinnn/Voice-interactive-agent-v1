package com.waynelinnn.voiceagent.domain.wake

/**
 * Soft wake-word matcher on STT finals (Sherpa).
 * Phrases: Quantis / Hey Quantis / 你好Quantis / 嘿Quantis / 嗨Quantis.
 */
object WakeWordMatcher {

    data class Match(
        /** Remaining user command after the wake phrase, may be blank. */
        val command: String,
    )

    private val wakePrefixes = listOf(
        Regex("""(?i)^\s*(hey|hi)\s*quantis\b[\s,，.。!！?？:：]*"""),
        Regex("""(?i)^\s*(你好|嘿|嗨)\s*quantis\b[\s,，.。!！?？:：]*"""),
        Regex("""(?i)^\s*quantis\b[\s,，.。!！?？:：]*"""),
    )

    private val containedNeedles = listOf(
        "heyquantis",
        "hiquantis",
        "你好quantis",
        "嘿quantis",
        "嗨quantis",
        "quantis",
    )

    fun match(transcript: String): Match? {
        val raw = transcript.trim()
        if (raw.isEmpty()) return null
        val normalized = normalize(raw)
        if (containedNeedles.none { normalized.contains(it) }) return null

        for (prefix in wakePrefixes) {
            if (prefix.containsMatchIn(raw)) {
                return Match(command = raw.replaceFirst(prefix, "").trim())
            }
        }
        // Wake word detected in normalized form but not as a clean prefix.
        return Match(command = "")
    }

    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("""[\s\p{Punct}，。！？、：；]+"""), "")
}
