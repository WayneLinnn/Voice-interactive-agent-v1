package com.waynelinnn.voiceagent.audio.capture

/**
 * Shared capture format for VAD / STT pipelines.
 */
object AudioCaptureConfig {
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNEL_COUNT = 1
    const val BYTES_PER_SAMPLE = 2
    const val FRAME_SAMPLES = 512 // 32 ms @ 16 kHz
}
