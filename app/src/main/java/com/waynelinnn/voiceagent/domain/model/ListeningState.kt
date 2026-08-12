package com.waynelinnn.voiceagent.domain.model

/**
 * Voice turn-taking state machine:
 * Idle → Listening → SpeechDetected → Recognizing → Thinking → Speaking → Listening
 * Speaking + user speech → SpeechDetected (barge-in)
 */
enum class ListeningState {
    Idle,
    Listening,
    SpeechDetected,
    Recognizing,
    Thinking,
    Speaking,
    Paused,
    Error,
}
