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

    /**
     * Se mantiene la referencia hasta que llega `VideoRecordEvent.Finalize`, para que
     * el `Recording` no sea recolectado por el GC durante la parada (esto emitiría
     * `ERROR_RECORDING_GARBAGE_COLLECTED` y dejaría la UI pegada en "Detener").
     */
    private var recording: Recording? = null
    private var stopRequested = false

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

    /**
     * Inicia una grabación. `onResult` se invoca siempre que llega `Finalize`,
     * con `success = false` si el evento trae cualquier error (no solo ERROR_NONE),
     * para que la UI pueda recuperarse en vez de quedarse en "Detener".
     */
    @SuppressLint("MissingPermission")
    fun start(output: File, onResult: (File, Boolean) -> Unit) {
        if (recording != null) return
        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        val hasMicrophone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCamera || !hasMicrophone) {
            onResult(output, false)
            return
        }

        val options = FileOutputOptions.Builder(output).build()
        stopRequested = false
        recording = try {
            controller.startRecording(
                options,
                AudioConfig.create(true),
                mainExecutor,
            ) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    recording = null
                    stopRequested = false
                    if (event.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                        onResult(output, true)
                    } else {
                        output.delete()
                        onResult(output, false)
                    }
                }
            }
        } catch (e: RuntimeException) {
            recording = null
            stopRequested = false
            output.delete()
            onResult(output, false)
            return
        }
        if (recording == null) {
            onResult(output, false)
        }
    }

    /**
     * Pide parar la grabación. Es idempotente: no se vuelve a llamar a
     * `Recording.stop()` si ya se pidió, evitando errores por doble parada.
     */
    fun stop() {
        val active = recording ?: return
        if (stopRequested) return
        stopRequested = true
        try {
            active.stop()
        } catch (e: IllegalStateException) {
            recording = null
            stopRequested = false
        }
    }
}
