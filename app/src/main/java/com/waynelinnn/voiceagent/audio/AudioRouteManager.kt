package com.waynelinnn.voiceagent.audio

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioRoute {
    Speaker,
    Earpiece,
    WiredHeadset,
    Bluetooth,
    Unknown,
}

data class AudioRouteSnapshot(
    val route: AudioRoute = AudioRoute.Unknown,
    val scoActive: Boolean = false,
    val bluetoothPermissionGranted: Boolean = true,
    val detail: String = "Detecting…",
)

/**
 * Basic wired / Bluetooth routing for voice sessions.
 * Prefers Bluetooth SCO when a headset is available; cleans up on session end.
 */
class AudioRouteManager(
    private val context: Context,
) {
    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _snapshot = MutableStateFlow(AudioRouteSnapshot())
    val snapshot: StateFlow<AudioRouteSnapshot> = _snapshot.asStateFlow()

    private var voiceRoutingEnabled = false
    private var receiversRegistered = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refreshAndMaybeActivateSco()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refreshAndMaybeActivateSco()
        }
    }

    private val headsetPlugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshAndMaybeActivateSco()
        }
    }

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) return
            val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            val scoOn = state == AudioManager.SCO_AUDIO_STATE_CONNECTED
            publish(
                route = if (scoOn) AudioRoute.Bluetooth else detectPreferredRoute(),
                scoActive = scoOn,
            )
        }
    }

    fun start() {
        if (!receiversRegistered) {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
            val plugFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
            ContextCompat.registerReceiver(
                appContext,
                headsetPlugReceiver,
                plugFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            val scoFilter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            ContextCompat.registerReceiver(
                appContext,
                scoReceiver,
                scoFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiversRegistered = true
        }
        voiceRoutingEnabled = true
        refreshAndMaybeActivateSco()
    }

    fun stop() {
        voiceRoutingEnabled = false
        stopSco()
        if (receiversRegistered) {
            runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
            runCatching { appContext.unregisterReceiver(headsetPlugReceiver) }
            runCatching { appContext.unregisterReceiver(scoReceiver) }
            receiversRegistered = false
        }
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        publish(route = detectPreferredRoute(), scoActive = false)
    }

    private fun refreshAndMaybeActivateSco() {
        val preferred = detectPreferredRoute()
        when {
            !voiceRoutingEnabled -> publish(preferred, scoActive = false)
            preferred == AudioRoute.Bluetooth && hasBluetoothConnectPermission() -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                startSco()
                publish(
                    route = AudioRoute.Bluetooth,
                    scoActive = audioManager.isBluetoothScoOn,
                )
            }
            preferred == AudioRoute.WiredHeadset -> {
                stopSco()
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                publish(route = AudioRoute.WiredHeadset, scoActive = false)
            }
            else -> {
                stopSco()
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                // Assistant-style default: loudspeaker when no headset.
                audioManager.isSpeakerphoneOn = true
                publish(route = AudioRoute.Speaker, scoActive = false)
            }
        }
    }

    private fun startSco() {
        if (!audioManager.isBluetoothScoAvailableOffCall) return
        if (!audioManager.isBluetoothScoOn) {
            runCatching {
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
            }
        }
    }

    private fun stopSco() {
        runCatching {
            if (audioManager.isBluetoothScoOn) {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
            }
        }
    }

    private fun detectPreferredRoute(): AudioRoute {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        val hasWired = devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        if (hasWired) return AudioRoute.WiredHeadset

        val hasBluetoothDevice = devices.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        } || isBluetoothHeadsetConnected()

        if (hasBluetoothDevice) return AudioRoute.Bluetooth

        // Emulators / some devices may omit earpiece; speaker is the safe default.
        return AudioRoute.Speaker
    }

    private fun isBluetoothHeadsetConnected(): Boolean {
        if (!hasBluetoothConnectPermission()) return false
        return runCatching {
            val manager = appContext.getSystemService(BluetoothManager::class.java) ?: return false
            val adapter = manager.adapter ?: return false
            if (!adapter.isEnabled) return false
            val proxies = manager.getConnectedDevices(BluetoothProfile.HEADSET)
            proxies.isNotEmpty() || adapter.getProfileConnectionState(BluetoothProfile.HEADSET) ==
                BluetoothAdapter.STATE_CONNECTED
        }.getOrDefault(false)
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun publish(route: AudioRoute, scoActive: Boolean) {
        val permissionGranted = hasBluetoothConnectPermission()
        _snapshot.value = AudioRouteSnapshot(
            route = route,
            scoActive = scoActive,
            bluetoothPermissionGranted = permissionGranted,
            detail = buildDetail(route, scoActive, permissionGranted),
        )
    }

    private fun buildDetail(
        route: AudioRoute,
        scoActive: Boolean,
        permissionGranted: Boolean,
    ): String = buildString {
        append(
            when (route) {
                AudioRoute.Speaker -> "Speaker"
                AudioRoute.Earpiece -> "Earpiece"
                AudioRoute.WiredHeadset -> "Wired headset"
                AudioRoute.Bluetooth -> "Bluetooth"
                AudioRoute.Unknown -> "Unknown"
            },
        )
        if (route == AudioRoute.Bluetooth) {
            append(if (scoActive) " · SCO on" else " · SCO off")
            if (!permissionGranted) append(" · need BLUETOOTH_CONNECT")
        }
    }
}
