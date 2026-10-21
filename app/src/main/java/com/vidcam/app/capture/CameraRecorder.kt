package com.vidcam.app.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
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
 * Controlador de cámara basado en CameraX que graba vídeo vertical con audio,
 * permite alternar la cámara frontal/trasera y activar/desactivar la estabilización
 * de imagen (EIS y OIS por hardware).
 *
 * Toda la clase está anotada con [ExperimentalCamera2Interop] porque usa
 * [Camera2CameraControl] para aplicar las opciones de captura de estabilización.
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
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

    var stabilizationEnabled: Boolean = true
        private set

    val isRecording: Boolean
        get() = recording != null

    fun setStabilization(enabled: Boolean) {
        stabilizationEnabled = enabled
        applyStabilization()
    }

    fun applyStabilization() {
        val control = controller.cameraControl ?: return
        try {
            val camera2Control = Camera2CameraControl.from(control)
            val mode = if (stabilizationEnabled) {
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
            } else {
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            }
            val oisMode = if (stabilizationEnabled) {
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
            } else {
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
            }
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, mode)
                .setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, oisMode)
                .build()
            camera2Control.setCaptureRequestOptions(options)
        } catch (_: Exception) {
            // Silencioso si el dispositivo o HAL no soporta opciones Camera2Interop
        }
    }

    fun toggleLens() {
        controller.cameraSelector =
            if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        applyStabilization()
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

        applyStabilization()

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
