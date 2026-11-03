package com.vidcam.app.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.vidcam.app.model.Caption
import com.vidcam.app.model.CaptionMath
import com.vidcam.app.model.CaptionStyle

/**
 * Overlay de un subtítulo: se dibuja como cualquier otra capa, con alfa según
 * su ventana de tiempo, y su centro cae en el mismo punto normalizado que usa la
 * vista previa.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class CaptionOverlay(
    private val bitmap: Bitmap,
    private val caption: Caption,
    private val yFraction: Float,
) : BitmapOverlay() {

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val visible = caption.isVisibleAt(presentationTimeUs / 1000L)
        return OverlaySettings.Builder()
            .setAlphaScale(if (visible) 1f else 0f)
            .setBackgroundFrameAnchor(0f, 1f - yFraction * 2f)
            .setOverlayFrameAnchor(0f, 0f)
            .build()
    }
}

/**
 * Rasteriza subtítulos en bitmaps del tamaño del fotograma de salida.
 *
 * El mismo texto se dibuja en la vista previa con las mismas fracciones, así
 * que el tamaño y la posición coinciden sin conversiones de densidad.
 */
object CaptionBitmaps {

    /** Ancho máximo del bloque de texto, como fracción del ancho del vídeo. */
    const val MAX_WIDTH_FRACTION = 0.86f

    private const val LINE_SPACING = 1.18f

    /**
     * Dibuja [caption] con [style] para un fotograma de [frameWidth]x[frameHeight].
     * Devuelve `null` si el texto está vacío.
     */
    fun build(
        caption: Caption,
        style: CaptionStyle,
        frameWidth: Int,
        frameHeight: Int,
    ): Bitmap? {
        val text = CaptionMath.displayText(caption, style)
        if (text.isBlank() || frameWidth <= 0 || frameHeight <= 0) return null
        val sizePx = (style.sizeFraction * frameHeight).coerceAtLeast(8f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = sizePx
            color = style.colorArgb
            textAlign = Paint.Align.LEFT
        }
        val maxWidth = frameWidth * MAX_WIDTH_FRACTION
        val lines = wrapLines(text, paint, maxWidth)
        if (lines.isEmpty()) return null

        val lineHeight = sizePx * LINE_SPACING
        val widest = lines.maxOf { paint.measureText(it) }
        val padHorizontal = sizePx * 0.38f
        val padVertical = sizePx * 0.22f
        val width = (widest + padHorizontal * 2f).coerceAtMost(maxWidth + padHorizontal * 2f)
        val height = lineHeight * lines.size + padVertical * 2f

        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1f).toInt(),
            height.coerceAtLeast(1f).toInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val background = style.backgroundArgb
        if (Color.alpha(background) > 0) {
            val box = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            val radius = sizePx * 0.28f
            canvas.drawRoundRect(box, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = background
            })
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = sizePx
            this.style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.09f
            color = Color.BLACK
        }
        var baseline = padVertical + sizePx
        for (line in lines) {
            val x = if (Color.alpha(background) > 0) {
                padHorizontal
            } else {
                (bitmap.width - paint.measureText(line)) / 2f
            }
            canvas.drawText(line, x, baseline, strokePaint)
            canvas.drawText(line, x, baseline, paint)
            baseline += lineHeight
        }
        return bitmap
    }

    /** Corta el texto en líneas que quepan en [maxWidth], midiendo con [paint]. */
    fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }
}
