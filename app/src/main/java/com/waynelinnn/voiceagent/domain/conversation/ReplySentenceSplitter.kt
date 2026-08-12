package com.waynelinnn.voiceagent.domain.conversation

/**
 * Pulls completed sentences from a streaming buffer for early TTS.
 * Boundaries: 。！？… . ! ? and newlines.
 */
object ReplySentenceSplitter {
    private val terminators = charArrayOf('。', '！', '？', '…', '.', '!', '?', '\n')

    fun drainComplete(buffer: StringBuilder): List<String> {
        if (buffer.isEmpty()) return emptyList()
        val out = ArrayList<String>(2)
        var start = 0
        var i = 0
        while (i < buffer.length) {
            if (buffer[i] in terminators) {
                val end = i + 1
                val piece = buffer.substring(start, end).trim()
                if (piece.isNotEmpty()) out.add(piece)
                start = end
            }
            i += 1
        }
        if (start > 0) {
            buffer.delete(0, start)
        }
        return out
    }
}
