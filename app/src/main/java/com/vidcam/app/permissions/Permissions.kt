package com.vidcam.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Permissions {

    private const val PREFS = "vidcam_permissions"
    private const val KEY_REQUESTED = "requested"

    /** Permisos esenciales solicitados en un único flujo al primer arranque. */
    val essential: List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun allGranted(context: Context): Boolean =
        essential.all { granted(context, it) }

    fun cameraGranted(context: Context): Boolean = granted(context, Manifest.permission.CAMERA)

    fun microphoneGranted(context: Context): Boolean =
        granted(context, Manifest.permission.RECORD_AUDIO)

    fun wasRequested(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REQUESTED, false)

    fun markRequested(context: Context) {
        prefs(context).edit().putBoolean(KEY_REQUESTED, true).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
