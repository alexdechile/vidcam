package com.vidcam.app.export

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.effect.VideoCompositorSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.vidcam.app.effect.*
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.model.Project
import com.vidcam.app.model.TimelineMath
import com.vidcam.app.model.VideoWindow
import com.vidcam.app.model.VideoWindowAspect
import com.vidcam.app.model.VideoWindowMath
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Motor de exportación basado en Media3 Transformer. Compone la línea de
 * tiempo (varios clips), la música opcional y las capas superpuestas en un
 * único MP4 H.264/AAC.
 *
 * Cuando el proyecto tiene fondos PNG o una ventana de vídeo distinta de la
 * original, se usa el compositor multi-entrada de Media3: los clips van primero
 * (capa superior) y los fondos después (capa inferior), y las capas PNG/texto se
 * siguen aplicando como overlay a nivel de composición.
 *
 * Un proyecto sin clips también se exporta: la secuencia de imágenes preparadas
 * (los fondos o un lienzo negro) hace de pista de vídeo durante
 * [Project.outputDurationMs], con las capas y la música por encima. Así se
 * pueden hacer vídeos solo con PNG.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class VideoExporter(private val context: Context) {

    interface Callback {
        fun onProgress(percent: Int)
        fun onSuccess(file: File)
        fun onError(message: String)
    }

    private data class PreparedBackdrop(
        val file: File,
        val startMs: Long,
        val endMs: Long,
    )

    private data class PreparedComposition(
        val dir: File,
        val backdrops: List<PreparedBackdrop>,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val prepareExecutor = Executors.newSingleThreadExecutor()

    private var transformer: Transformer? = null
    private var handler: Handler? = null

    @Volatile
    private var generation = 0

    private var tempDir: File? = null

    fun cancel() {
        generation++
        transformer?.cancel()
        transformer = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
        cleanupTemporaryFiles()
    }

    fun export(project: Project, callback: Callback) {
        if (!project.canExport) {
            callback.onError("Añade un vídeo, un fondo o una capa para exportar")
            return
        }
        if (project.outputDurationMs <= 0L) {
            callback.onError("El proyecto no tiene duración")
            return
        }

        val token = generation
        // Sin clips hace falta igualmente una imagen base que haga de vídeo,
        // aunque el proyecto no tenga fondos (se usa un lienzo negro).
        if (project.clips.isNotEmpty() && !project.requiresVideoComposition) {
            startExport(
                project = project,
                frameSize = FrameGeometry.frameSize(context, project),
                prepared = null,
                callback = callback,
            )
            return
        }

        // Preparar los fondos implica decodificar imágenes: se hace fuera del
        // hilo principal y se vuelve a él para arrancar el Transformer.
        prepareExecutor.execute {
            val prepared = prepareComposition(project, token)
            mainHandler.post {
                if (token != generation) {
                    prepared?.dir?.deleteRecursively()
                    return@post
                }
                if (prepared == null) {
                    callback.onError("No se pudo preparar una imagen de fondo")
                    return@post
                }
                startExport(
                    project = project,
                    frameSize = FrameGeometry.frameSize(context, project),
                    prepared = prepared,
                    callback = callback,
                )
            }
        }
    }

    private fun startExport(
        project: Project,
        frameSize: Pair<Int, Int>,
        prepared: PreparedComposition?,
        callback: Callback,
    ) {
        val (videoWidth, videoHeight) = frameSize
        tempDir = prepared?.dir

        val sequences = buildSequences(
            project = project,
            prepared = prepared,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
        )
        val compositionBuilder = Composition.Builder(sequences)

        if (prepared != null && project.clips.isNotEmpty()) {
            val settings = buildCompositorSettings(
                project = project,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
            )
            compositionBuilder.setVideoCompositorSettings(settings)
        }

        // Ordenar capas por zIndex (menor primero = más abajo) para que
            // la exportación coincida con la vista previa.
            val sortedLayers = project.layers.sortedBy { it.zIndex }
            val overlays: List<TextureOverlay> = sortedLayers.mapNotNull {
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
        cleanupTemporaryFiles()
    }

    private fun cleanupTemporaryFiles() {
        tempDir?.deleteRecursively()
        tempDir = null
    }

    /**
     * Prepara en segundo plano los fondos y los tamaños de los clips. Devuelve
     * `null` si una imagen de fondo no se puede leer.
     */
    private fun prepareComposition(project: Project, token: Int): PreparedComposition? {
        val (width, height) = FrameGeometry.frameSize(context, project)
        val dir = File(context.cacheDir, "composition/${System.currentTimeMillis()}-$token")
            .apply { mkdirs() }
        val segments = VideoWindowMath.backgroundSegments(
            backgrounds = project.backgrounds,
            totalMs = project.outputDurationMs,
        )
        val backdrops = mutableListOf<PreparedBackdrop>()
        var blackFile: File? = null

        for ((index, segment) in segments.withIndex()) {
            if (generation != token) {
                dir.deleteRecursively()
                return null
            }
            val background = segment.background
            val file = if (background == null) {
                blackFile ?: BackgroundBitmaps.blackFile(dir, width, height)?.also {
                    blackFile = it
                }
            } else {
                BackgroundBitmaps.prepareFile(
                    context = context,
                    uriString = background.uri,
                    width = width,
                    height = height,
                    dir = dir,
                    name = "fondo-$index-${background.id}",
                )
            }
            if (file == null) {
                dir.deleteRecursively()
                return null
            }
            backdrops += PreparedBackdrop(file, segment.startMs, segment.endMs)
        }

        return PreparedComposition(dir, backdrops)
    }

    private fun buildCompositorSettings(
        project: Project,
        videoWidth: Int,
        videoHeight: Int,
    ): VideoCompositorSettings =
        WindowCompositorSettings(
            outputWidth = videoWidth,
            outputHeight = videoHeight,
            window = project.videoWindow,
        )

    /**
     * [SpeedProvider] de velocidad constante para cámara rápida/lenta en la exportación.
     */
    private class ConstantSpeed(private val speed: Float) : SpeedProvider {
        override fun getSpeed(timeUs: Long): Float = speed
        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
    }

    private class WindowCompositorSettings(
        private val outputWidth: Int,
        private val outputHeight: Int,
        private val window: VideoWindow,
    ) : VideoCompositorSettings {

        override fun getOutputSize(inputSizes: List<Size>): Size =
            Size(outputWidth, outputHeight)

        override fun getOverlaySettings(inputId: Int, presentationTimeUs: Long): OverlaySettings =
            if (inputId == 0) {
                // Entrada primaria: la ventana de vídeo, encima del fondo.
                OverlaySettings.Builder()
                    .setBackgroundFrameAnchor(
                        window.x * 2f - 1f,
                        1f - window.y * 2f,
                    )
                    .setOverlayFrameAnchor(0f, 0f)
                    // Los clips se normalizan a `windowSize(..., 1f)`, así que
                    // la escala relativa de la ventana es directamente la del
                    // compositor.
                    .setScale(window.scale, window.scale)
                    // Compose rota en sentido horario; Media3 antihorario.
                    .setRotationDegrees(-window.rotationDeg)
                    .build()
            } else {
                // Fondos ya recortados al tamaño del fotograma: escala 1.
                OverlaySettings.Builder()
                    .setBackgroundFrameAnchor(0f, 0f)
                    .setOverlayFrameAnchor(0f, 0f)
                    .setScale(1f, 1f)
                    .build()
            }
    }

    private fun buildSequences(
        project: Project,
        prepared: PreparedComposition?,
        videoWidth: Int,
        videoHeight: Int,
    ): List<EditedMediaItemSequence> {
        val window = project.videoWindow
        val windowAspect = VideoWindowMath.aspectRatio(
            aspect = window.aspect,
            originalAspect = videoWidth.toFloat() / videoHeight.coerceAtLeast(1).toFloat(),
        )
        // Todos los clips se normalizan al mayor tamaño de la ventana (escala 1)
        // con recorte *cover*. Así el compositor ve texturas del mismo tamaño y
        // la escala de la ventana es exactamente `window.scale`.
        val (baseWidth, baseHeight) = VideoWindowMath.windowSize(
            frameWidth = videoWidth.toFloat(),
            frameHeight = videoHeight.toFloat(),
            aspect = windowAspect,
            scale = 1f,
        )
        val baseW = baseWidth.roundToInt().coerceAtLeast(2)
        val baseH = baseHeight.roundToInt().coerceAtLeast(2)
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
            // Una instancia por clip: las transformaciones guardan estado al
            // configurarse y compartirlas entre clips las corrompería.
            val presentationEffect = if (prepared == null) {
                null
            } else {
                Presentation.createForWidthAndHeight(
                    baseW,
                    baseH,
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
                )
            }
            val wideEffect = clip.wideLens.media3Transformation()
            val shakeEffect = clip.shake.media3Transformation()
            val hasVideoEffects = speed != 1f || colorEffect != null ||
                presentationEffect != null || wideEffect != null || shakeEffect != null
            if (hasVideoEffects) {
                // En Media3 1.5 la velocidad se pide con efectos: el par
                // audio/vídeo de createExperimentalSpeedChangingEffect mantiene
                // la sincronización; el filtro de color, la normalización de la
                // ventana y el encuadre ancho se componen como efectos de vídeo.
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
                    presentationEffect?.let { add(it) }
                    wideEffect?.let { add(it) }
                    shakeEffect?.let { add(it) }
                }
                val audioEffects =
                    if (speedPair != null) listOf(speedPair.first) else emptyList()
                builder.setEffects(Effects(audioEffects, videoEffects))
            }
            builder.build()
        }

        // La entrada primaria (índice 0) se dibuja encima: los clips van primero
        // y la secuencia de fondos después. Sin clips, el lienzo preparado es la
        // única pista de vídeo.
        val sequences = mutableListOf<EditedMediaItemSequence>()
        if (project.clips.isNotEmpty()) {
            sequences += EditedMediaItemSequence.Builder(videoItems).build()
        }

        if (prepared != null) {
            val backdropItems = prepared.backdrops.map { backdrop ->
                val durationMs = (backdrop.endMs - backdrop.startMs).coerceAtLeast(1L)
                EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(backdrop.file)))
                    .setDurationUs(durationMs * 1000L)
                    .setFrameRate(30)
                    .setRemoveAudio(true)
                    .build()
            }
            if (backdropItems.isNotEmpty()) {
                sequences += EditedMediaItemSequence.Builder(backdropItems).build()
            }
        }

        val music = project.music
        val windowMs = TimelineMath.musicWindowMs(project)
        if (music != null && windowMs != null) {
            val (start, end) = windowMs
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
