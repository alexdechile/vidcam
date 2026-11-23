package com.vidcam.app.export

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
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
import com.vidcam.app.audio.EnvelopeAudioProcessor
import com.vidcam.app.audio.GainAudioProcessor
import com.vidcam.app.audio.PcmAudio
import com.vidcam.app.audio.WavFile
import com.vidcam.app.effect.*
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.model.AnimationSpeed
import com.vidcam.app.model.KenBurnsMath
import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.Project
import com.vidcam.app.model.AudioMixMath
import com.vidcam.app.model.TimelineMath
import com.vidcam.app.model.VideoWindow
import com.vidcam.app.model.VideoWindowAspect
import com.vidcam.app.model.VideoWindowMath
import com.vidcam.app.model.VoiceoverMath
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Imagen de fondo ya preparada para el compositor.
 *
 * [startMs] y [endMs] son el tramo del compositor (el hueco que esa imagen cubre
 * en la secuencia de fondos); [kenBurns] y su ventana `kenBurnsStartMs..
 * kenBurnsEndMs` son los del fondo original, para que el zoom de la exportación
 * sea idéntico al de la vista previa aunque el tramo se haya recortado por un
 * solape.
 */
private data class PreparedBackdrop(
    val file: File,
    val startMs: Long,
    val endMs: Long,
    val kenBurns: KenBurnsMode = KenBurnsMode.NINGUNO,
    val kenBurnsSpeed: AnimationSpeed = AnimationSpeed.RAPIDO,
    val kenBurnsStartMs: Long = 0L,
    val kenBurnsEndMs: Long = 0L,
    val kenBurnsIndex: Int = 0,
)

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

    private companion object {
        const val TAG = "VidCamExport"
    }

    interface Callback {
        fun onProgress(percent: Int)
        fun onLog(line: String) {}
        fun onSuccess(file: File)
        fun onError(message: String)
    }

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

    /** Carpeta temporal de la pista de voz preparada (relleno o recorte). */
    private var voiceTempDir: File? = null

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
        val startElapsedMs = SystemClock.elapsedRealtime()

        // La validación de archivos y la decodificación de fondos se hacen
        // fuera del hilo principal para no trabar la pantalla y registrar cada paso.
        prepareExecutor.execute {
            val frameSize = FrameGeometry.frameSize(context, project)
            ExportLog.summarizeProject(project, frameSize.first, frameSize.second)
                .forEach { emitLog(callback, startElapsedMs, it) }

            val clipError = verifyClips(project, callback, startElapsedMs)
            if (clipError != null) {
                postErrorIfCurrent(token, callback, clipError)
                return@execute
            }

            val musicError = verifyMusic(project, callback, startElapsedMs)
            if (musicError != null) {
                postErrorIfCurrent(token, callback, musicError)
                return@execute
            }

            val needsBackdrops = project.clips.isEmpty() || project.requiresVideoComposition
            val prepared = if (needsBackdrops) {
                emitLog(callback, startElapsedMs, "Preparando fondos en PNG…")
                prepareComposition(project, token, callback, startElapsedMs)
            } else {
                null
            }

            mainHandler.post {
                if (token != generation) {
                    prepared?.dir?.deleteRecursively()
                    return@post
                }
                if (needsBackdrops && prepared == null) {
                    emitLog(callback, startElapsedMs, "FALLO: no se pudo decodificar un fondo")
                    callback.onError("No se pudo preparar una imagen de fondo")
                    return@post
                }
                startExport(
                    project = project,
                    frameSize = frameSize,
                    prepared = prepared,
                    callback = callback,
                    startElapsedMs = startElapsedMs,
                )
            }
        }
    }

    private fun postErrorIfCurrent(token: Int, callback: Callback, message: String) {
        mainHandler.post {
            if (token == generation) callback.onError(message)
        }
    }

    private fun emitLog(callback: Callback, startElapsedMs: Long, message: String) {
        val line = ExportLog.formatLine(SystemClock.elapsedRealtime() - startElapsedMs, message)
        Log.i(TAG, line)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.onLog(line)
        } else {
            mainHandler.post { callback.onLog(line) }
        }
    }

    private fun verifyClips(
        project: Project,
        callback: Callback,
        startElapsedMs: Long,
    ): String? {
        for ((index, clip) in project.clips.withIndex()) {
            val parsed = Uri.parse(clip.uri)
            if (parsed.scheme == "file" || parsed.scheme == null) {
                val path = parsed.path
                val file = path?.let(::File)
                if (file == null || !file.exists() || file.length() <= 0L) {
                    emitLog(
                        callback,
                        startElapsedMs,
                        "FALLO: clip #${index + 1} no encontrado (${clip.uri})",
                    )
                    return "No se encontró el archivo del clip #${index + 1}"
                }
            }
            emitLog(
                callback,
                startElapsedMs,
                "Clip #${index + 1}: ${clip.trimStartMs}..${clip.trimEndMs} ms · " +
                    "vel=${clip.playbackSpeed}x · filtro=${clip.colorFilter} · " +
                    "lente=${clip.wideLens} · temblor=${clip.shake}",
            )
        }
        return null
    }

    private fun verifyMusic(
        project: Project,
        callback: Callback,
        startElapsedMs: Long,
    ): String? {
        val music = project.music ?: return null
        val windowMs = TimelineMath.musicWindowMs(project) ?: return null
        if (project.musicVolume <= 0f) return null
        val uri = Uri.parse(music.uri)
        return try {
            val mime = context.contentResolver.getType(uri)
            val ok = if (uri.scheme == "file" || uri.scheme == null) {
                val file = uri.path?.let(::File)
                file != null && file.exists() && file.length() > 0L
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
            }
            if (!ok) {
                emitLog(
                    callback,
                    startElapsedMs,
                    "FALLO: pista de música inaccesible (${music.uri})",
                )
                "No se puede leer la pista de música elegida"
            } else {
                emitLog(
                    callback,
                    startElapsedMs,
                    "Música lista: \"${music.title}\" (${windowMs.first}..${windowMs.second} ms, " +
                        "bucle=${TimelineMath.musicShouldLoop(project)}, tipo=${mime ?: "local"})",
                )
                null
            }
        } catch (e: Exception) {
            emitLog(
                callback,
                startElapsedMs,
                "FALLO: error al abrir música (${e.javaClass.simpleName}: ${e.message})",
            )
            "No se puede leer la pista de música elegida"
        }
    }

    private fun startExport(
        project: Project,
        frameSize: Pair<Int, Int>,
        prepared: PreparedComposition?,
        callback: Callback,
        startElapsedMs: Long,
    ) {
        val (videoWidth, videoHeight) = frameSize
        tempDir = prepared?.dir
        val voiceWorkDir = File(context.cacheDir, "voz-${System.currentTimeMillis()}")
            .apply { mkdirs() }
        voiceTempDir = voiceWorkDir

        // La toma se lee una sola vez: se usa para armar la pista de voz y para
        // calcular la envolvente del ducking de la música.
        val voiceAudio = readVoiceAudio(project)
        val ducking = buildDucking(project, voiceAudio)
        if (project.voiceover?.isUsable == true) {
            emitLog(
                callback,
                startElapsedMs,
                if (voiceAudio != null) {
                    "Voz en off cargada: ${voiceAudio.durationMs} ms @ ${voiceAudio.sampleRate} Hz"
                } else {
                    "Aviso: no se pudo leer el archivo de voz en off"
                },
            )
        }

        val sequences = buildSequences(
            project = project,
            prepared = prepared,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            voiceWorkDir = voiceWorkDir,
            voiceAudio = voiceAudio,
            ducking = ducking,
        )
        emitLog(callback, startElapsedMs, "Secuencias armadas para Media3: ${sequences.size}")
        val compositionBuilder = Composition.Builder(sequences)
        if (project.clips.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Evita fallos al mezclar clips HDR del teléfono con fondos o capas SDR.
            compositionBuilder.setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
            emitLog(callback, startElapsedMs, "Modo de color: conversión HDR→SDR por OpenGL")
        }

        if (prepared != null && project.clips.isNotEmpty()) {
            val settings = buildCompositorSettings(
                project = project,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
            )
            compositionBuilder.setVideoCompositorSettings(settings)
            emitLog(
                callback,
                startElapsedMs,
                "Compositor de ventana: ${videoWidth}x$videoHeight · " +
                    "escala=${project.videoWindow.scale} · aspecto=${project.videoWindow.aspect}",
            )
        }

        // Ordenar capas por zIndex (menor primero = más abajo) para que
        // la exportación coincida con la vista previa.
        val sortedLayers = project.layers.sortedBy { it.zIndex }
        val layerOverlays: List<TextureOverlay> = sortedLayers.mapNotNull {
            LayerBitmaps.buildOverlay(context, it, videoWidth, videoHeight)
        }
        val captionOverlays = buildCaptionOverlays(project, videoWidth, videoHeight)
        val overlays: List<TextureOverlay> = layerOverlays + captionOverlays
        if (overlays.isNotEmpty()) {
            val overlayEffect: Effect = OverlayEffect(overlays)
            compositionBuilder.setEffects(Effects(emptyList(), listOf(overlayEffect)))
            emitLog(
                callback,
                startElapsedMs,
                "Overlays listos: ${layerOverlays.size}/${project.layers.size} capa(s), " +
                    "${captionOverlays.size} subtítulo(s)",
            )
        }
        val composition = compositionBuilder.build()

        val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val output = File(outputDir, "vidcam-${System.currentTimeMillis()}.mp4")

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                stopPolling()
                emitLog(
                    callback,
                    startElapsedMs,
                    "MP4 generado (${output.length() / 1024} KB)",
                )
                callback.onSuccess(output)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                stopPolling()
                Log.e(TAG, "Fallo al exportar", exportException)
                emitLog(
                    callback,
                    startElapsedMs,
                    "Error Media3 [${exportException.getErrorCodeName()}]: ${exportException.message}",
                )
                var depth = 1
                var cause = exportException.cause
                while (cause != null && depth <= 4) {
                    emitLog(
                        callback,
                        startElapsedMs,
                        "Causa $depth (${cause.javaClass.simpleName}): ${cause.message}",
                    )
                    cause = cause.cause
                    depth++
                }
                callback.onError(describe(exportException))
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
        var lastMilestone = -1
        val poller = object : Runnable {
            override fun run() {
                if (transformer !== built) return
                if (built.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val p = progressHolder.progress
                    callback.onProgress(p)
                    val milestone = (p / 25) * 25
                    if (milestone > lastMilestone && milestone in 0..100) {
                        lastMilestone = milestone
                        emitLog(callback, startElapsedMs, "Progreso de render: $p %")
                    }
                }
                mainHandler.postDelayed(this, 200L)
            }
        }
        mainHandler.postDelayed(poller, 200L)

        try {
            emitLog(callback, startElapsedMs, "Arrancando motor Media3 Transformer…")
            built.start(composition, output.absolutePath)
        } catch (e: Exception) {
            stopPolling()
            Log.e(TAG, "No se pudo iniciar la exportación", e)
            emitLog(
                callback,
                startElapsedMs,
                "Excepción al arrancar (${e.javaClass.simpleName}): ${e.message}",
            )
            callback.onError(describe(e))
        }
    }

    /**
     * Mensaje de error con el nombre del código de Media3 y las causas
     * encadenadas: `ExportException` suele traer solo "Asset loader error" y el
     * motivo real (MIME no soportado, archivo ilegible, códec, mezcla de
     * `ColorInfo`) queda en la causa.
     */
    private fun describe(exception: Throwable): String {
        val head = buildString {
            append(exception.message?.trim()?.takeIf { it.isNotEmpty() } ?: "Error al exportar")
            if (exception is ExportException) {
                append(" [").append(exception.getErrorCodeName()).append(']')
            }
        }
        val causes = generateSequence(exception.cause) { it.cause }
            .take(3)
            .mapNotNull { it.message?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .toList()
        return (listOf(head) + causes).joinToString(" · ").take(320)
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
        voiceTempDir?.deleteRecursively()
        voiceTempDir = null
    }

    /**
     * Prepara en segundo plano los fondos y los tamaños de los clips. Devuelve
     * `null` si una imagen de fondo no se puede leer.
     */
    private fun prepareComposition(
        project: Project,
        token: Int,
        callback: Callback,
        startElapsedMs: Long,
    ): PreparedComposition? {
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
                emitLog(
                    callback,
                    startElapsedMs,
                    "FALLO: no se pudo preparar tramo #$index (${background?.uri ?: "lienzo negro"})",
                )
                dir.deleteRecursively()
                return null
            }
            emitLog(
                callback,
                startElapsedMs,
                "Fondo #$index: ${background?.label ?: "Negro"} (${segment.startMs}..${segment.endMs} ms, " +
                    "kenBurns=${background?.kenBurns ?: KenBurnsMode.NINGUNO}, ${file.length() / 1024} KB)",
            )
            backdrops += PreparedBackdrop(
                file = file,
                startMs = segment.startMs,
                endMs = segment.endMs,
                kenBurns = background?.kenBurns ?: KenBurnsMode.NINGUNO,
                kenBurnsSpeed = background?.kenBurnsSpeed ?: AnimationSpeed.RAPIDO,
                kenBurnsStartMs = background?.startMs ?: 0L,
                kenBurnsEndMs = background?.endMs ?: 0L,
                kenBurnsIndex = KenBurnsMath.indexOf(project.backgrounds, background?.id),
            )
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
                // Fondos ya recortados al tamaño del fotograma: escala 1. El Ken
                // Burns de cada fondo va como efecto del propio elemento, antes
                // de llegar al compositor (ver `KenBurnsTransformation`).
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
        voiceWorkDir: File,
        voiceAudio: PcmAudio?,
        ducking: FloatArray?,
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
            val muteOriginal = project.originalAudioMuted || project.originalVolume <= 0f
            if (muteOriginal) {
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
            // Volumen del audio original: se aplica como efecto de audio por
            // clip (Media3 no expone volumen por pista en la composición).
            val originalGain = if (muteOriginal) 1f else project.originalVolume
            // En Media3 1.5 la velocidad se pide con efectos: el par
            // audio/vídeo de createExperimentalSpeedChangingEffect mantiene
            // la sincronización; el filtro de color, la normalización de la
            // ventana y el encuadre ancho se componen como efectos de vídeo.
            val speedPair =
                if (!muteOriginal && speed != 1f) {
                    Effects.createExperimentalSpeedChangingEffect(ConstantSpeed(speed))
                } else {
                    null
                }
            val videoEffects = buildList {
                when {
                    speed == 1f -> Unit
                    muteOriginal -> add(SpeedChangeEffect(speed))
                    else -> speedPair?.second?.let { add(it) }
                }
                colorEffect?.let { add(it) }
                presentationEffect?.let { add(it) }
                wideEffect?.let { add(it) }
                shakeEffect?.let { add(it) }
            }
            val audioEffects = buildList {
                speedPair?.first?.let { add(it) }
                if (!muteOriginal && originalGain != 1f) {
                    add(GainAudioProcessor(originalGain.coerceAtLeast(0.01f)))
                }
            }
            if (audioEffects.isNotEmpty() || videoEffects.isNotEmpty()) {
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
                // Media3 requiere el tipo MIME y la duración de imagen en el
                // MediaItem para que ImageAssetLoader no falle en versiones 1.5+.
                val backdropMediaItem = MediaItem.Builder()
                    .setUri(Uri.fromFile(backdrop.file))
                    .setMimeType(MimeTypes.IMAGE_PNG)
                    .setImageDurationMs(durationMs)
                    .build()
                val builder = EditedMediaItem.Builder(backdropMediaItem)
                    .setDurationUs(durationMs * 1000L)
                    .setFrameRate(30)
                    .setRemoveAudio(true)
                if (KenBurnsMath.isActive(backdrop.kenBurns)) {
                    // Una instancia por elemento: guarda el origen de su propio
                    // reloj, así que no se puede compartir entre fondos.
                    builder.setEffects(
                        Effects(
                            emptyList(),
                            listOf(
                                KenBurnsTransformation(
                                    mode = backdrop.kenBurns,
                                    speed = backdrop.kenBurnsSpeed,
                                    index = backdrop.kenBurnsIndex,
                                    itemStartMs = backdrop.startMs,
                                    startMs = backdrop.kenBurnsStartMs,
                                    endMs = backdrop.kenBurnsEndMs,
                                ),
                            ),
                        ),
                    )
                }
                builder.build()
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
            val musicBuilder = EditedMediaItem.Builder(musicItem)
                .setRemoveVideo(true)
            val musicGain = project.musicVolume.coerceIn(0f, 1f)
            if (musicGain > 0f) {
                val totalMs = project.outputDurationMs
                val hasFades = project.musicFadeInMs > 0L || project.musicFadeOutMs > 0L
                val envelope: (Long) -> Float = { positionMs ->
                    AudioMixMath.trackGain(
                        positionMs = positionMs,
                        lengthMs = totalMs,
                        fadeInMs = project.musicFadeInMs,
                        fadeOutMs = project.musicFadeOutMs,
                        ducking = ducking,
                    ) * musicGain
                }
                if (musicGain != 1f || hasFades || ducking != null) {
                    musicBuilder.setEffects(
                        Effects(listOf(EnvelopeAudioProcessor(envelope)), emptyList()),
                    )
                }
                sequences += EditedMediaItemSequence.Builder(musicBuilder.build())
                    .setIsLooping(TimelineMath.musicShouldLoop(project))
                    .build()
            }
        }

        val voiceItem = buildVoiceItem(project, voiceWorkDir, voiceAudio)
        if (voiceItem != null) {
            sequences += EditedMediaItemSequence.Builder(voiceItem).build()
        }

        return sequences
    }

    /** Lee la toma de voz del proyecto, si existe y tiene audio. */
    private fun readVoiceAudio(project: Project): PcmAudio? {
        val voice = project.voiceover ?: return null
        if (!voice.isUsable) return null
        val path = Uri.parse(voice.activeUri).path ?: return null
        val file = File(path)
        if (!file.exists()) return null
        return WavFile.read(file)
    }

    /**
     * Envolvente de ducking: cuánto baja la música en cada bloque de 50 ms,
     * calculada con el nivel de la voz ya colocada en la línea de tiempo.
     */
    private fun buildDucking(project: Project, voiceAudio: PcmAudio?): FloatArray? {
        val voice = project.voiceover ?: return null
        val amount = project.duckingAmount
        if (amount <= 0f || !voice.isUsable) return null
        val audio = voiceAudio ?: return null
        val plan = VoiceoverMath.plan(voice, project.outputDurationMs)
        if (plan.isEmpty) return null
        val peaks = AudioMixMath.timelinePeaks(
            samples = audio.samples,
            sampleRate = audio.sampleRate,
            padMs = plan.padMs,
            skipMs = plan.skipMs,
            projectMs = project.outputDurationMs,
        )
        return AudioMixMath.duckingEnvelope(peaks, amount)
    }

    /** Rasteriza los subtítulos visibles como overlays de la composición. */
    private fun buildCaptionOverlays(
        project: Project,
        videoWidth: Int,
        videoHeight: Int,
    ): List<TextureOverlay> {
        if (project.captions.isEmpty()) return emptyList()
        val style = project.captionStyle.bounded()
        return project.captions.mapNotNull { caption ->
            CaptionBitmaps.build(caption, style, videoWidth, videoHeight)?.let { bitmap ->
                CaptionOverlay(bitmap, caption, style.yFraction)
            }
        }
    }

    /**
     * Prepara la pista de voz en off: escribe un WAV con el relleno y el
     * recorte que la colocan en su segundo dentro del vídeo, así el mezclador
     * de secuencias de Media3 la suma al audio original y a la música.
     *
     * Los fundidos y el volumen se aplican con una envolvente sobre el tramo
     * audible (después del relleno), no sobre el silencio previo.
     *
     * Sin voz utilizable, o si el WAV no se puede leer, el proyecto se exporta
     * igual que antes.
     */
    private fun buildVoiceItem(
        project: Project,
        workDir: File,
        voiceAudio: PcmAudio?,
    ): EditedMediaItem? {
        val voice = project.voiceover ?: return null
        if (!voice.isUsable || voice.volume <= 0f) return null
        val plan = VoiceoverMath.plan(voice, project.outputDurationMs)
        if (plan.isEmpty) return null
        val audio = voiceAudio ?: return null
        val sampleRate = audio.sampleRate
        val padSamples = (plan.padMs * sampleRate / 1000L).toInt()
        val skipSamples = (plan.skipMs * sampleRate / 1000L).toInt()
        val takeSamples = (plan.takeMs * sampleRate / 1000L).toInt()
        if (takeSamples <= 0) return null
        val file = File(workDir, "voz-${System.currentTimeMillis()}.wav")
        if (!WavFile.writeSegment(file, audio.samples, sampleRate, padSamples, skipSamples, takeSamples)) {
            return null
        }
        val builder = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(file)))
            .setRemoveVideo(true)
        val hasFades = voice.fadeInMs > 0L || voice.fadeOutMs > 0L
        if (voice.volume != 1f || hasFades) {
            val contentMs = plan.takeMs
            val envelope: (Long) -> Float = { positionMs ->
                val local = (positionMs - plan.padMs).coerceIn(0L, contentMs)
                AudioMixMath.fadeGain(
                    positionMs = local,
                    lengthMs = contentMs,
                    fadeInMs = voice.fadeInMs,
                    fadeOutMs = voice.fadeOutMs,
                ) * voice.volume
            }
            builder.setEffects(Effects(listOf(EnvelopeAudioProcessor(envelope)), emptyList()))
        }
        return builder.build()
    }
}
