package com.vidcam.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.net.Uri
import java.io.File

/**
 * GIF animado rasterizado en una lista corta de fotogramas, para que la vista
 * previa y el MP4 exportado muestren el mismo movimiento.
 *
 * `android.graphics.Movie` sigue siendo la única API del sistema capaz de
 * decodificar un GIF cuadro a cuadro, pero solo dibuja sobre un `Canvas` en un
 * instante dado. Para no redibujar en cada fotograma del vídeo (y para que la
 * exportación sea barata) los fotogramas se rasterizan una vez al cargar el
 * archivo, muestreando su bucle a [SAMPLES_PER_SECOND] con un tope de
 * [MAX_FRAMES] imágenes y [MAX_SIDE] píxeles por lado.
 *
 * El emparejamiento tiempo→fotograma es una función pura
 * ([frameIndexFor]) que se prueba con tests de JVM.
 */
class GifAnimation private constructor(
    val width: Int,
    val height: Int,
    private val frames: List<Bitmap>,
    private val loopMs: Long,
) {

    val frameCount: Int
        get() = frames.size

    val firstFrame: Bitmap
        get() = frames.first()

    fun frameIndexAt(timeMs: Long): Int = frameIndexFor(timeMs, loopMs, frames.size)

    /** Fotograma que corresponde a [timeMs] dentro de la línea de tiempo. */
    fun bitmapAt(timeMs: Long): Bitmap = frames[frameIndexAt(timeMs)]

    companion object {

        /** Lado máximo del fotograma rasterizado; acota la memoria de un GIF grande. */
        const val MAX_SIDE = 384

        /** Fotogramas que se guardan como mucho por GIF. */
        const val MAX_FRAMES = 24

        /** Muestras por segundo del bucle del GIF. */
        const val SAMPLES_PER_SECOND = 10

        private const val GIF_HEADER_LENGTH = 6

        private val cache = object : LinkedHashMap<String, GifAnimation>(2, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GifAnimation>) =
                size > 2
        }

        /**
         * Fotograma que toca mostrar en [timeMs]. El GIF repite su bucle, así
         * que el tiempo se acota al ciclo antes de repartirlo entre fotogramas.
         */
        fun frameIndexFor(timeMs: Long, loopMs: Long, frameCount: Int): Int {
            if (frameCount <= 1 || loopMs <= 0L) return 0
            val local = ((timeMs % loopMs) + loopMs) % loopMs
            val slice = loopMs.toDouble() / frameCount.toDouble()
            return (local / slice).toInt().coerceIn(0, frameCount - 1)
        }

        /** `true` si [file] es un GIF (cabecera `GIF87a`/`GIF89a`). */
        fun isGif(file: File): Boolean = try {
            file.inputStream().use { stream ->
                val header = ByteArray(GIF_HEADER_LENGTH)
                val read = stream.read(header)
                read == GIF_HEADER_LENGTH && isGifHeader(header)
            }
        } catch (e: Exception) {
            false
        }

        /** `true` si la URI apunta a un GIF legible. */
        fun isGif(context: Context, uriString: String): Boolean =
            readBytes(context, uriString)?.let { bytes ->
                bytes.size > GIF_HEADER_LENGTH && isGifHeader(bytes)
            } ?: false

        private fun isGifHeader(bytes: ByteArray): Boolean {
            val header = String(bytes, 0, GIF_HEADER_LENGTH, Charsets.US_ASCII)
            return header == "GIF87a" || header == "GIF89a"
        }

        /**
         * Carga (y cachea) el GIF de [uriString]. Devuelve `null` si el archivo
         * no es un GIF legible o no tiene tamaño.
         */
        @Synchronized
        fun load(context: Context, uriString: String): GifAnimation? {
            cache[uriString]?.let { return it }
            val animation = try {
                decode(context, uriString)
            } catch (e: Exception) {
                null
            }
            if (animation != null) cache[uriString] = animation
            return animation
        }

        private fun decode(context: Context, uriString: String): GifAnimation? {
            val bytes = readBytes(context, uriString) ?: return null
            if (bytes.size <= GIF_HEADER_LENGTH || !isGifHeader(bytes)) return null
            val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val sourceWidth = movie.width()
            val sourceHeight = movie.height()
            if (sourceWidth <= 0 || sourceHeight <= 0) return null

            val scale = minOf(
                1f,
                MAX_SIDE.toFloat() / maxOf(sourceWidth, sourceHeight).toFloat(),
            )
            val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
            val height = (sourceHeight * scale).toInt().coerceAtLeast(1)

            val loopMs = movie.duration().toLong().takeIf { it > 0L }
                ?.coerceAtLeast(1L)
                ?: 0L
            // Un GIF "estático" (un solo cuadro) se dibuja una única vez.
            val sampleCount = if (loopMs <= 0L) {
                1
            } else {
                (loopMs * SAMPLES_PER_SECOND / 1000L).toInt().coerceIn(1, MAX_FRAMES)
            }

            val frames = (0 until sampleCount).map { index ->
                val gifTimeMs = if (sampleCount == 1) 0L else loopMs * index / sampleCount
                drawFrame(movie, gifTimeMs, width, height)
            }
            return GifAnimation(width, height, frames, if (loopMs <= 0L) 1L else loopMs)
        }

        private fun drawFrame(
            movie: Movie,
            timeMs: Long,
            width: Int,
            height: Int,
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)
            canvas.save()
            canvas.scale(
                width.toFloat() / movie.width().toFloat(),
                height.toFloat() / movie.height().toFloat(),
            )
            movie.setTime(timeMs.toInt())
            movie.draw(canvas, 0f, 0f)
            canvas.restore()
            return bitmap
        }

        private fun readBytes(context: Context, uriString: String): ByteArray? = try {
            val uri = Uri.parse(uriString)
            val stream = if (uri.scheme == "content" || uri.scheme == "android.resource") {
                context.contentResolver.openInputStream(uri)
            } else {
                File(uri.path ?: uriString).takeIf { it.exists() }?.inputStream()
            }
            stream?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}
