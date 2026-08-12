package com.waynelinnn.voiceagent.presentation.permission

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.waynelinnn.voiceagent.R

data class VoicePermissionController(
    val snapshot: VoicePermissionSnapshot,
    val bluetoothGranted: Boolean,
    val requestVoicePermissions: () -> Unit,
    val requestHeadsetPermission: (onDone: () -> Unit) -> Unit,
    val openSettings: () -> Unit,
)

@Composable
fun rememberVoicePermissionController(): VoicePermissionController {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var snapshot by remember {
        mutableStateOf(context.voicePermissionSnapshot(activity))
    }
    var bluetoothGranted by remember {
        mutableStateOf(AppPermissions.optionalHeadset().all(context::hasPermission))
    }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var requestedOnce by remember { mutableStateOf(false) }
    var pendingAfterHeadset by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun refresh() {
        snapshot = context.voicePermissionSnapshot(activity)
        bluetoothGranted = AppPermissions.optionalHeadset().all(context::hasPermission)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        requestedOnce = true
        refresh()
        val micGranted = result[AppPermissions.recordAudio] == true ||
            context.hasPermission(AppPermissions.recordAudio)
        if (!micGranted) {
            val permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    AppPermissions.recordAudio,
                )
            if (permanentlyDenied) {
                showSettingsDialog = true
            }
        }
        pendingAfterHeadset?.invoke()
        pendingAfterHeadset = null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(text = stringResource(R.string.permission_denied_title)) },
            text = { Text(text = stringResource(R.string.permission_denied_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        context.openAppSettings()
                    },
                ) {
                    Text(text = stringResource(R.string.permission_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(text = stringResource(R.string.permission_not_now))
                }
            },
        )
    }

    return VoicePermissionController(
        snapshot = snapshot,
        bluetoothGranted = bluetoothGranted,
        requestVoicePermissions = {
            val missing = AppPermissions.requiredForVoiceSession()
                .filterNot(context::hasPermission)
            if (missing.isEmpty()) {
                refresh()
                return@VoicePermissionController
            }
            if (requestedOnce &&
                activity != null &&
                !context.hasPermission(AppPermissions.recordAudio) &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    AppPermissions.recordAudio,
                )
            ) {
                showSettingsDialog = true
            } else {
                launcher.launch(missing.toTypedArray())
            }
        },
        requestHeadsetPermission = { onDone ->
            val missing = AppPermissions.optionalHeadset().filterNot(context::hasPermission)
            if (missing.isEmpty()) {
                onDone()
            } else {
                pendingAfterHeadset = onDone
                launcher.launch(missing.toTypedArray())
            }
        },
        openSettings = context::openAppSettings,
    )
}
