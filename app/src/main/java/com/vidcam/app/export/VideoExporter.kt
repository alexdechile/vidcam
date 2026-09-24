package com.vidcam.app.export

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.vidcam.app.effect.media3Effect
import com.vidcam.app.effect.media3Transformation
import com.vidcam.app.model.Project
import com.vidcam.app.model.TimelineMath
import java.io.File

/**
 * Motor de exportación basado en Media3 Transformer. Compone la línea de
 * tiempo (varios clips), la música opcional y las capas superpuestas en un
 * único MP4 H.264/AAC.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class VideoExporter(private val context: Context) {

    interface Callback {
        fun onProgress(percent: Int)
        fun onSuccess(file: File)
        fun onError(message: String)
    }

    private var transformer: Transformer? = null
    private var handler: Handler? = null

    fun cancel() {
        transformer?.cancel()
        transformer = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    fun export(project: Project, callback: Callback) {
        if (project.clips.isEmpty()) {
            callback.onError("Añade al menos un vídeo a la línea de tiempo")
            return
        }

        val sequences = buildSequences(project)
        val compositionBuilder = Composition.Builder(sequences)

        val (videoWidth, videoHeight) = FrameGeometry.frameSize(context, project)

        val overlays: List<TextureOverlay> = project.layers.mapNotNull {
            LayerBitmaps.buildOverlay(context, it, videoWidth, videoHeight)
        }
        if (overlays.isNotEmpty()) {
            val overlayEffect: Effect = OverlayEffect(overlays)
            compositionBuilder.setEffects(Effects(emptyList(), listOf(overlayEffect)))
        }
        val composition = compositionBuilder.build()

        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val output = File(outputDir, "vidcam-${System.currentTimeMillis()}.mp4")

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                stopPolling()
                callback.onSuccess(output)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                stopPolling()
                callback.onError(exportException.message ?: "Error desconocido al exportar")
            }
        }

        val built = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()
        transformer = built

        val progressHolder = ProgressHolder()
        val mainHandler = Handler(Looper.getMainLooper())
        handler = mainHandler
        val poller = object : Runnable {
            override fun run() {
                if (transformer !== built) return
                if (built.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    callback.onProgress(progressHolder.progress)
                }
                mainHandler.postDelayed(this, 200L)
            }
        }
        mainHandler.postDelayed(poller, 200L)

        try {
            built.start(composition, output.absolutePath)
        } catch (e: Exception) {
            stopPolling()
            callback.onError(e.message ?: "No se pudo iniciar la exportación")
        }
    }

    private fun stopPolling() {
        transformer = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    /** [SpeedProvider] de velocidad constante para cámara rápida/lenta en la exportación. */
    private class ConstantSpeed(private val speed: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = speed
        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }

    private fun buildSequences(project: Project): List<EditedMediaItemSequence> {
        val videoItems = project.clips.map { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build(),
                )
                .build()
            val speed = clip.playbackSpeed.coerceAtLeast(0.01f)
            val builder = EditedMediaItem.Builder(mediaItem)
            if (project.originalAudioMuted) {
                builder.setRemoveAudio(true)
            }
            val colorEffect = clip.colorFilter.media3Effect()
            val wideEffect = clip.wideLens.media3Transformation()
            val hasVideoEffects = speed != 1f || colorEffect != null || wideEffect != null
            if (hasVideoEffects) {
                // En Media3 1.5 la velocidad se pide con efectos: el par
                // audio/vídeo de createExperimentalSpeedChangingEffect mantiene
                // la sincronización; el filtro de color y el encuadre ancho se
                // componen como efectos de vídeo adicionales.
                val speedPair =
                    if (!project.originalAudioMuted && speed != 1f) {
                        Effects.createExperimentalSpeedChangingEffect(ConstantSpeed(speed))
                    } else {
                        null
                    }
                val videoEffects = buildList {
                    when {
                        speed == 1f -> Unit
                        project.originalAudioMuted -> add(SpeedChangeEffect(speed))
                        else -> speedPair?.second?.let { add(it) }
                    }
                    colorEffect?.let { add(it) }
                    wideEffect?.let { add(it) }
                }
                val audioEffects =
                    if (speedPair != null) listOf(speedPair.first) else emptyList()
                builder.setEffects(Effects(audioEffects, videoEffects))
            }
            builder.build()
        }
        val sequences = mutableListOf(
            EditedMediaItemSequence.Builder(videoItems).build(),
        )

        val music = project.music
        val window = TimelineMath.musicWindowMs(project)
        if (music != null && window != null) {
            val (start, end) = window
            val musicItem = MediaItem.Builder()
                .setUri(music.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(start)
                        .setEndPositionMs(end)
                        .build(),
                )
                .build()
            val editedMusic = EditedMediaItem.Builder(musicItem)
                .setRemoveVideo(true)
                .build()
            sequences += EditedMediaItemSequence.Builder(editedMusic)
                .setIsLooping(TimelineMath.musicShouldLoop(project))
                .build()
        }

        return sequences
    }
}
