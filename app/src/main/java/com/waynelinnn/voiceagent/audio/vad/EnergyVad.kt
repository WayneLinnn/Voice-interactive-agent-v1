package com.waynelinnn.voiceagent.audio.vad

sealed interface VadEvent {
    data object SpeechStarted : VadEvent
    data object SpeechEnded : VadEvent
}

/**
 * Lightweight energy-gate VAD for Module 3 step 2.
 * Silero ONNX can replace this later behind the same event API.
 */
class EnergyVad(
    private val startThreshold: Double = 1_200.0,
    private val endThreshold: Double = 700.0,
    private val startFrames: Int = 3, // ~96 ms
    private val endFrames: Int = 18, // ~576 ms silence
    private val onEvent: (VadEvent) -> Unit,
) {
    private var inSpeech = false
    private var voicedRun = 0
    private var silenceRun = 0

    fun reset() {
        inSpeech = false
        voicedRun = 0
        silenceRun = 0
    }

    fun accept(frame: ShortArray) {
        if (frame.isEmpty()) return
        val rms = rms(frame)
        if (!inSpeech) {
            if (rms >= startThreshold) {
                voicedRun += 1
                if (voicedRun >= startFrames) {
                    inSpeech = true
                    silenceRun = 0
                    onEvent(VadEvent.SpeechStarted)
                }
            } else {
                voicedRun = 0
            }
        } else {
            if (rms <= endThreshold) {
                silenceRun += 1
                if (silenceRun >= endFrames) {
                    inSpeech = false
                    voicedRun = 0
                    silenceRun = 0
                    onEvent(VadEvent.SpeechEnded)
                }
            } else {
                silenceRun = 0
            }
        }
    }

    private fun rms(frame: ShortArray): Double {
        var sum = 0.0
        for (sample in frame) {
            val value = sample.toDouble()
            sum += value * value
        }
        return kotlin.math.sqrt(sum / frame.size)
    }
}
