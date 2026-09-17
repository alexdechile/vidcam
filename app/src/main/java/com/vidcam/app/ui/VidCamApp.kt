package com.vidcam.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vidcam.app.editor.EditorScreen
import com.vidcam.app.editor.EditorViewModel
import com.vidcam.app.permissions.Permissions

@Composable
fun VidCamApp() {
    val context = LocalContext.current
    var mediaGranted by remember { mutableStateOf(Permissions.mediaGranted(context)) }
    var canRecord by remember { mutableStateOf(Permissions.recordingGranted(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        mediaGranted = Permissions.mediaGranted(context)
        canRecord = Permissions.recordingGranted(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        mediaGranted = Permissions.mediaGranted(context)
        canRecord = Permissions.recordingGranted(context)
    }

    LaunchedEffect(Unit) {
        if (!mediaGranted && !Permissions.wasRequested(context)) {
            Permissions.markRequested(context)
            launcher.launch(Permissions.essential.toTypedArray())
        }
    }

    if (mediaGranted) {
        val editorViewModel: EditorViewModel = viewModel()
        EditorScreen(
            viewModel = editorViewModel,
            canRecord = canRecord,
            onRequestPermissions = {
                launcher.launch(Permissions.essential.toTypedArray())
            },
        )
    } else {
        PermissionScreen(
            onRequest = { launcher.launch(Permissions.essential.toTypedArray()) },
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            },
        )
    }
}
