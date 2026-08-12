package com.waynelinnn.voiceagent.data.remote.stream

/**
 * Placeholder for provider WebSocket streaming.
 * Real duplex voice/token streaming can plug in here later.
 */
interface WebSocketStreamClient {
    fun isSupported(): Boolean = false
}
