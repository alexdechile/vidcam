package com.vidcam.app.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Controlador de cámara basado en CameraX que graba vídeo vertical con audio
 * y permite alternar la cámara frontal/trasera.
 */
class CameraRecorder(rawContext: Context) {

    private val context: Context = rawContext.applicationContext

    val controller: LifecycleCameraController = LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var recording: Recording? = null

    val isRecording: Boolean
        get() = recording != null

    fun toggleLens() {
        controller.cameraSelector =
            if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
    }

    @SuppressLint("MissingPermission")
    fun start(output: File, onFinalized: (File) -> Unit) {
        if (recording != null) return
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val hasMicrophone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera || !hasMicrophone) return

        val options = FileOutputOptions.Builder(output).build()
        recording = controller.startRecording(
            options,
            AudioConfig.create(true),
            mainExecutor,
        ) { event ->
            if (event is VideoRecordEvent.Finalize) {
                recording = null
                if (event.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                    onFinalized(output)
                }
            }
        }
    }

    fun stop() {
        recording?.stop()
        recording = null
    }
}
