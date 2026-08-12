package com.waynelinnn.voiceagent.presentation.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

enum class PermissionGrant {
    Granted,
    Denied,
    PermanentlyDenied,
}

data class VoicePermissionSnapshot(
    val microphone: PermissionGrant,
    val notifications: PermissionGrant,
) {
    val isVoiceReady: Boolean
        get() = microphone == PermissionGrant.Granted &&
            (AppPermissions.postNotifications == null || notifications == PermissionGrant.Granted)
}

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.voicePermissionSnapshot(activity: Activity? = null): VoicePermissionSnapshot {
    val mic = permissionGrant(AppPermissions.recordAudio, activity)
    val notificationsPermission = AppPermissions.postNotifications
    val notifications = if (notificationsPermission == null) {
        PermissionGrant.Granted
    } else {
        permissionGrant(notificationsPermission, activity)
    }
    return VoicePermissionSnapshot(
        microphone = mic,
        notifications = notifications,
    )
}

fun Context.permissionGrant(permission: String, activity: Activity?): PermissionGrant {
    if (hasPermission(permission)) return PermissionGrant.Granted
    val shouldShowRationale = activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    return if (shouldShowRationale) {
        PermissionGrant.Denied
    } else {
        // First ask OR "Don't ask again" — UI treats missing grant after a request as permanent.
        PermissionGrant.Denied
    }
}

fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
