package com.waynelinnn.voiceagent.data.remote

/**
 * Network endpoints and defaults for OpenAI-compatible chat APIs.
 * Concrete provider URLs can override [baseUrl] later.
 */
object NetworkConfig {
    const val DEFAULT_BASE_URL = "https://api.openai.com/"
    const val CHAT_COMPLETIONS_PATH = "v1/chat/completions"
    const val SPEECH_PATH = "v1/audio/speech"
    const val CONNECT_TIMEOUT_SECONDS = 20L
    const val READ_TIMEOUT_SECONDS = 60L
    const val WRITE_TIMEOUT_SECONDS = 30L
}
