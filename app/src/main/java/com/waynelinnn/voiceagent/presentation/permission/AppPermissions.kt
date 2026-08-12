package com.waynelinnn.voiceagent.presentation.permission

import android.Manifest
import android.os.Build

/**
 * Central permission catalog for Module 2.
 * Runtime requests are gated by [requiredForVoiceSession]; Bluetooth is declared now
 * and requested when headset routing is wired in a later step.
 */
object AppPermissions {
    val recordAudio = Manifest.permission.RECORD_AUDIO

    val postNotifications: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    val bluetoothConnect: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            null
        }

    /** Must be granted before entering a voice session. */
    fun requiredForVoiceSession(): Array<String> = buildList {
        add(recordAudio)
        postNotifications?.let(::add)
    }.toTypedArray()

    /** Declared for later headset SCO work; not blocking home entry yet. */
    fun optionalHeadset(): Array<String> = buildList {
        bluetoothConnect?.let(::add)
    }.toTypedArray()
}
