package com.waynelinnn.voiceagent.domain.model

/**
 * Voice turn-taking state machine:
 * Idle → WakeListening? → Listening → SpeechDetected → Recognizing → Thinking → Speaking → Listening
 * Speaking + user speech → SpeechDetected (barge-in)
 */
enum class ListeningState {
    Idle,
    /** Armed for wake phrase (e.g. “Hey Quantis”); finals without match are ignored. */
    WakeListening,
    Listening,
    SpeechDetected,
    Recognizing,
    Thinking,
    Speaking,
    Paused,
    Error,
}
