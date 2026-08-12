package com.waynelinnn.voiceagent.domain.performance

import android.os.SystemClock
import android.util.Log

/** Lightweight listen→think→speak timing marks (Logcat tag VoiceLatency). */
object VoiceLatencyTrace {
    private const val TAG = "VoiceLatency"

    fun mark(label: String, sinceMs: Long? = null) {
        val now = SystemClock.elapsedRealtime()
        if (sinceMs == null) {
            Log.i(TAG, "$label at=${now}")
        } else {
            Log.i(TAG, "$label +${now - sinceMs}ms (t=$now)")
        }
    }
}
