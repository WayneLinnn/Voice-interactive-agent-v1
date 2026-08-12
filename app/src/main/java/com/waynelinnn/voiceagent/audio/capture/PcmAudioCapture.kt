package com.waynelinnn.voiceagent.audio.capture

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Captures mono PCM16 @ 16 kHz frames for downstream VAD/STT.
 */
class PcmAudioCapture(
    private val onFrame: (ShortArray) -> Unit,
    private val onError: (Throwable) -> Unit = {},
) {
    private val running = AtomicBoolean(false)
    private var recordThread: Thread? = null
    private var audioRecord: AudioRecord? = null

    val isRunning: Boolean
        get() = running.get()

    @SuppressLint("MissingPermission")
    fun start() {
        if (!running.compareAndSet(false, true)) return

        val minBuffer = AudioRecord.getMinBufferSize(
            AudioCaptureConfig.SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            running.set(false)
            onError(IllegalStateException("Invalid AudioRecord buffer size: $minBuffer"))
            return
        }

        val bufferSize = maxOf(minBuffer, AudioCaptureConfig.FRAME_SAMPLES * 2)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                AudioCaptureConfig.SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (error: Exception) {
            running.set(false)
            onError(error)
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            running.set(false)
            onError(IllegalStateException("AudioRecord failed to initialize"))
            return
        }

        audioRecord = recorder
        recorder.startRecording()

        recordThread = thread(name = "pcm-audio-capture", priority = Thread.MAX_PRIORITY) {
            val frame = ShortArray(AudioCaptureConfig.FRAME_SAMPLES)
            try {
                while (running.get()) {
                    val read = recorder.read(frame, 0, frame.size)
                    if (read > 0) {
                        val copy = if (read == frame.size) {
                            frame.copyOf()
                        } else {
                            frame.copyOf(read)
                        }
                        onFrame(copy)
                    } else if (read < 0) {
                        onError(IllegalStateException("AudioRecord read failed: $read"))
                        break
                    }
                }
            } catch (error: Exception) {
                if (running.get()) {
                    Log.e(TAG, "Capture loop failed", error)
                    onError(error)
                }
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        try {
            audioRecord?.run {
                runCatching { stop() }
                release()
            }
        } finally {
            audioRecord = null
            recordThread = null
        }
    }

    companion object {
        private const val TAG = "PcmAudioCapture"
    }
}
