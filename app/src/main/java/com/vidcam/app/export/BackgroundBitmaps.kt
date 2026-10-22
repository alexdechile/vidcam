package com.vidcam.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * Prepara las imágenes de fondo para el compositor de Media3.
 *
 * El compositor dibuja cada entrada a su tamaño nativo en píxeles, así que los
 * fondos se recortan (*cover*) y escalan exactamente al tamaño del fotograma de
 * salida. De ese modo la vista previa (`ContentScale.Crop`) y el MP4 coinciden y
 * no hace falta calcular nada por fotograma.
 */
object BackgroundBitmaps {

    /** PNG negro con el tamaño del fotograma, para los tramos sin fondo. */
    fun blackFile(dir: File, width: Int, height: Int): File? = try {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.RGB_565,
        )
        bitmap.eraseColor(Color.BLACK)
        val file = File(dir, "fondo-negro-${width}x$height.png")
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        file.takeIf { it.length() > 0L }
    } catch (e: Exception) {
        null
    }

    /**
     * Decodifica [uriString], aplica la orientación EXIF, recorta al centro para
     * llenar [width]x[height] y guarda un PNG temporal.
     */
    fun prepareFile(
        context: Context,
        uriString: String,
        width: Int,
        height: Int,
        dir: File,
        name: String,
    ): File? {
        val targetWidth = width.coerceAtLeast(1)
        val targetHeight = height.coerceAtLeast(1)
        val bitmap = decodeCover(context, uriString, targetWidth, targetHeight) ?: return null
        return try {
            val file = File(dir, "$name.png")
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            file.takeIf { it.length() > 0L }
        } catch (e: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeCover(
        context: Context,
        uriString: String,
        width: Int,
        height: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, uriString)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Se muestrea solo mientras el resultado siga teniendo de sobra el
        // tamaño pedido, para no cargar una foto de 50 MP en memoria.
        var sample = 1
        while (
            bounds.outWidth / (sample * 2) >= width * 2 ||
            bounds.outHeight / (sample * 2) >= height * 2
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = openStream(context, uriString)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val oriented = applyOrientation(context, uriString, decoded)

        val sourceAspect = oriented.width.toFloat() / oriented.height.toFloat()
        val targetAspect = width.toFloat() / height.toFloat()
        val sourceRect = if (sourceAspect > targetAspect) {
            val cropWidth = (oriented.height * targetAspect).toInt().coerceIn(1, oriented.width)
            val left = (oriented.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, oriented.height)
        } else {
            val cropHeight = (oriented.width / targetAspect).toInt().coerceIn(1, oriented.height)
            val top = (oriented.height - cropHeight) / 2
            Rect(0, top, oriented.width, top + cropHeight)
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(oriented, sourceRect, Rect(0, 0, width, height), paint)
        if (oriented !== output) oriented.recycle()
        if (decoded !== oriented) decoded.recycle()
        return output
    }

    private fun applyOrientation(
        context: Context,
        uriString: String,
        bitmap: Bitmap,
    ): Bitmap {
        val orientation = try {
            openStream(context, uriString)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun openStream(context: Context, uriString: String): InputStream? = try {
        if (uriString.startsWith(LayerBitmaps.ASSET_SCHEME)) {
            context.assets.open(uriString.removePrefix(LayerBitmaps.ASSET_SCHEME))
        } else {
            context.contentResolver.openInputStream(Uri.parse(uriString))
        }
    } catch (e: Exception) {
        null
    }
}
