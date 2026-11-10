package com.vidcam.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.vidcam.app.effect.classicEffect
import com.vidcam.app.media.GifAnimation
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.model.ColorFilterPreset
import com.vidcam.app.model.KenBurnsMath
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.LayerTiming
import com.vidcam.app.model.Project
import com.vidcam.app.model.VideoClip
import com.vidcam.app.model.VideoWindowAspect
import com.vidcam.app.model.VideoWindowMath
import com.vidcam.app.model.resolvedAlphaAt
import com.vidcam.app.model.resolvedAt
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Renderizador de fotogramas compuestos en alta resolución sin pérdida (PNG).
 *
 * Compone el fondo, la ventana de vídeo con su filtro de color, las capas
 * (PNG, GIF, texto) con animaciones procedimentales/movimiento y los subtítulos
 * en el instante exacto indicado sobre un lienzo `ARGB_8888`.
 */
object FrameExporter {

    /**
     * Renderiza un fotograma compuesto de la composición en [timeMs].
     */
    fun renderFrame(
        context: Context,
        project: Project,
        timeMs: Long = 0L,
        targetWidth: Int = FrameGeometry.DEFAULT_WIDTH,
        targetHeight: Int = FrameGeometry.DEFAULT_HEIGHT,
    ): Bitmap {
        val width = targetWidth.coerceAtLeast(2)
        val height = targetHeight.coerceAtLeast(2)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Fondo (color negro o PNG con cobertura completa)
        drawBackground(context, canvas, project, timeMs, width, height)

        // 2. Ventana de vídeo
        if (project.clips.isNotEmpty()) {
            drawVideoWindow(context, canvas, project, timeMs, width, height)
        }

        // 3. Capas superpuestas (PNG, GIF, Texto)
        drawLayers(context, canvas, project, timeMs, width, height)

        // 4. Subtítulos quemados
        drawCaption(canvas, project, timeMs, width, height)

        return bitmap
    }

    /**
     * Exporta el fotograma en [timeMs] como archivo PNG con compresión sin pérdidas.
     */
    fun exportPng(
        context: Context,
        project: Project,
        timeMs: Long,
        outputFile: File,
    ): Boolean {
        val bitmap = renderFrame(context, project, timeMs)
        return try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawBackground(
        context: Context,
        canvas: Canvas,
        project: Project,
        timeMs: Long,
        width: Int,
        height: Int,
    ) {
        val activeBg = VideoWindowMath.backgroundAt(project.backgrounds, timeMs)
            ?: project.backgrounds.firstOrNull()
        val bgBitmap = activeBg?.uri?.let { LayerBitmaps.decodePng(context, it) }

        if (bgBitmap != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val matrix = Matrix()

            val scale = maxOf(
                width.toFloat() / bgBitmap.width.coerceAtLeast(1),
                height.toFloat() / bgBitmap.height.coerceAtLeast(1),
            )
            val dx = (width - bgBitmap.width * scale) * 0.5f
            val dy = (height - bgBitmap.height * scale) * 0.5f
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)

            // Aplicar Ken Burns si está activo
            if (activeBg != null && KenBurnsMath.isActive(activeBg.kenBurns)) {
                val totalDuration = (activeBg.endMs - activeBg.startMs).coerceAtLeast(1L)
                val elapsed = (timeMs - activeBg.startMs).coerceIn(0L, totalDuration)
                val progress = elapsed.toFloat() / totalDuration.toFloat()
                val kbTransform = KenBurnsMath.resolve(
                    mode = activeBg.kenBurns,
                    progress = progress,
                    aspectRatio = width.toFloat() / height.toFloat(),
                    speed = activeBg.speed,
                )
                matrix.postScale(
                    kbTransform.scale,
                    kbTransform.scale,
                    width * 0.5f,
                    height * 0.5f,
                )
                matrix.postTranslate(
                    kbTransform.anchorX * width,
                    kbTransform.anchorY * height,
                )
            }

            canvas.drawBitmap(bgBitmap, matrix, paint)
        } else {
            canvas.drawColor(Color.BLACK)
        }
    }

    private fun drawVideoWindow(
        context: Context,
        canvas: Canvas,
        project: Project,
        timeMs: Long,
        frameWidth: Int,
        frameHeight: Int,
    ) {
        val (activeClip, clipOffsetMs) = findClipAt(project.clips, timeMs) ?: return
        val clipUri = Uri.parse(activeClip.uri)
        val sourceTimeMs = activeClip.trimStartMs +
            (clipOffsetMs * activeClip.playbackSpeed.coerceAtLeast(0.01f)).toLong()
        val timeUs = sourceTimeMs * 1000L

        val frameBitmap = MediaProbe.extractFrame(context, clipUri, timeUs) ?: return
        try {
            val videoWindow = project.videoWindow
            val originalAspect = frameBitmap.width.toFloat() / frameBitmap.height.coerceAtLeast(1).toFloat()
            val windowAspect = VideoWindowMath.aspectRatio(videoWindow.aspect, originalAspect)

            val (windowW, windowH) = VideoWindowMath.windowSize(
                frameWidth = frameWidth.toFloat(),
                frameHeight = frameHeight.toFloat(),
                aspect = windowAspect,
                scale = videoWindow.scale,
            )

            val cx = (0.5f + videoWindow.translationX) * frameWidth
            val cy = (0.5f + videoWindow.translationY) * frameHeight

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            if (activeClip.colorFilter != ColorFilterPreset.NINGUNO) {
                val effect = classicEffect(activeClip.colorFilter)
                if (effect != null) {
                    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(effect.toCompose()))
                }
            }

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(videoWindow.rotationDeg)

            val dstRect = RectF(-windowW * 0.5f, -windowH * 0.5f, windowW * 0.5f, windowH * 0.5f)

            // Recorte 'cover' dentro de la ventana de aspecto
            val srcAspect = frameBitmap.width.toFloat() / frameBitmap.height.coerceAtLeast(1).toFloat()
            val srcRect = if (srcAspect > windowAspect) {
                val takeWidth = (frameBitmap.height * windowAspect).roundToInt()
                val left = ((frameBitmap.width - takeWidth) * 0.5f).roundToInt()
                Rect(left, 0, left + takeWidth, frameBitmap.height)
            } else {
                val takeHeight = (frameBitmap.width / windowAspect).roundToInt()
                val top = ((frameBitmap.height - takeHeight) * 0.5f).roundToInt()
                Rect(0, top, frameBitmap.width, top + takeHeight)
            }

            canvas.drawBitmap(frameBitmap, srcRect, dstRect, paint)
            canvas.restore()
        } finally {
            frameBitmap.recycle()
        }
    }

    private fun findClipAt(clips: List<VideoClip>, timeMs: Long): Pair<VideoClip, Long>? {
        if (clips.isEmpty()) return null
        var accumulated = 0L
        for (clip in clips) {
            val duration = clip.effectiveDurationMs
            if (timeMs >= accumulated && timeMs < accumulated + duration) {
                return clip to (timeMs - accumulated)
            }
            accumulated += duration
        }
        return clips.first() to 0L
    }

    private fun drawLayers(
        context: Context,
        canvas: Canvas,
        project: Project,
        timeMs: Long,
        width: Int,
        height: Int,
    ) {
        val visibleLayers = project.layers.filter { LayerTiming.isVisibleAt(it, timeMs) }

        for (layer in visibleLayers) {
            val animated = layer.resolvedAt(timeMs)
            val alpha = layer.resolvedAlphaAt(timeMs)
            if (alpha <= 0.005f) continue

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.alpha = (alpha * 255f).roundToInt().coerceIn(0, 255)
            }

            val layerBitmap: Bitmap? = when (layer.kind) {
                LayerKind.PNG -> layer.uri?.let { LayerBitmaps.decodePng(context, it) }
                LayerKind.GIF -> layer.uri?.let { GifAnimation.load(context, it)?.bitmapAt(timeMs) }
                LayerKind.TEXT -> {
                    val textSizePx = minOf(width, height) * LayerBitmaps.BASE_TEXT_SIZE_FRACTION *
                        (layer.fontSizeSp / LayerBitmaps.BASE_TEXT_SIZE_SP)
                    LayerBitmaps.renderText(layer, textSizePx * LayerBitmaps.TEXT_RASTER_FACTOR)
                }
            }

            if (layerBitmap != null) {
                val baseScale: Float = when (layer.kind) {
                    LayerKind.PNG, LayerKind.GIF -> {
                        val targetWidth = width * LayerBitmaps.BASE_PNG_WIDTH_FRACTION
                        targetWidth / layerBitmap.width.coerceAtLeast(1)
                    }
                    LayerKind.TEXT -> 1f / LayerBitmaps.TEXT_RASTER_FACTOR
                }

                val finalScale = baseScale * animated.scale
                val posX = animated.x * width
                val posY = animated.y * height

                canvas.save()
                canvas.translate(posX, posY)
                canvas.rotate(animated.rotationDeg)
                canvas.scale(finalScale, finalScale)

                val left = -layerBitmap.width * 0.5f
                val top = -layerBitmap.height * 0.5f
                canvas.drawBitmap(layerBitmap, left, top, paint)
                canvas.restore()

                if (layer.kind == LayerKind.TEXT) {
                    layerBitmap.recycle()
                }
            }
        }
    }

    private fun drawCaption(
        canvas: Canvas,
        project: Project,
        timeMs: Long,
        width: Int,
        height: Int,
    ) {
        val caption = project.captionAt(timeMs) ?: return
        val captionBitmap = CaptionBitmaps.build(
            caption = caption,
            style = project.captionStyle,
            frameWidth = width,
            frameHeight = height,
        ) ?: return

        try {
            val yPos = project.captionStyle.verticalPositionFraction * height - captionBitmap.height * 0.5f
            val xPos = (width - captionBitmap.width) * 0.5f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(captionBitmap, xPos, yPos, paint)
        } finally {
            captionBitmap.recycle()
        }
    }
}
