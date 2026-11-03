package com.vidcam.app.data

import android.content.Context
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Configuración del servicio de transcripción.
 *
 * La app no trae credenciales ni un servicio propio: se apunta a cualquier
 * endpoint que acepte un `multipart/form-data` con el campo `file`, como el
 * Whisper local del homelab (`http://equipo:3007/transcribe`) o la API de
 * OpenAI (`https://api.openai.com/v1/audio/transcriptions`).
 */
data class TranscriptionSettings(
    val url: String = "",
    val token: String = "",
    /** Idioma de la toma; vacío = lo detecta el servicio. */
    val language: String = "es",
    /** Modelo a pedir; se omite si está vacío. */
    val model: String = "",
) {
    val isConfigured: Boolean
        get() = url.startsWith("http://") || url.startsWith("https://")
}

/** Guarda la configuración de transcripción en las preferencias de la app. */
class TranscriptionSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("transcripcion", Context.MODE_PRIVATE)

    fun load(): TranscriptionSettings = TranscriptionSettings(
        url = prefs.getString(KEY_URL, "").orEmpty(),
        token = prefs.getString(KEY_TOKEN, "").orEmpty(),
        language = prefs.getString(KEY_LANGUAGE, "es").orEmpty(),
        model = prefs.getString(KEY_MODEL, "").orEmpty(),
    )

    fun save(settings: TranscriptionSettings) {
        prefs.edit()
            .putString(KEY_URL, settings.url.trim())
            .putString(KEY_TOKEN, settings.token.trim())
            .putString(KEY_LANGUAGE, settings.language.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .apply()
    }

    private companion object {
        const val KEY_URL = "url"
        const val KEY_TOKEN = "token"
        const val KEY_LANGUAGE = "language"
        const val KEY_MODEL = "model"
    }
}

/** Resultado de una transcripción: los tramos o el motivo del fallo. */
sealed interface TranscriptionResult {
    data class Success(val segments: List<TranscriptSegment>) : TranscriptionResult
    data class Failure(val message: String) : TranscriptionResult
}

/**
 * Envía la toma a un endpoint de transcripción compatible con Whisper.
 *
 * Usa `HttpURLConnection` a propósito: agregar OkHttp por una sola llamada no
 * se justifica y la app hoy no tiene cliente HTTP propio.
 */
class TranscriptionClient(private val settings: TranscriptionSettings) {

    fun transcribe(file: File, durationMs: Long): TranscriptionResult {
        if (!settings.isConfigured) {
            return TranscriptionResult.Failure("Configura primero el servicio de transcripción")
        }
        if (!file.exists() || file.length() == 0L) {
            return TranscriptionResult.Failure("No hay una toma de voz para transcribir")
        }
        val boundary = "----vidcam${System.currentTimeMillis()}"
        val connection = try {
            (URL(settings.url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 180_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
                if (settings.token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${settings.token}")
                }
            }
        } catch (e: Exception) {
            return TranscriptionResult.Failure("URL inválida: ${e.message}")
        }

        return try {
            writeBody(connection.outputStream, file, boundary)
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = body.take(300).ifBlank { "sin detalle" }
                TranscriptionResult.Failure("El servicio respondió $code: $detail")
            } else {
                val segments = TranscriptParser.parse(body, durationMs)
                if (segments.isEmpty()) {
                    TranscriptionResult.Failure("El servicio no devolvió texto")
                } else {
                    TranscriptionResult.Success(segments)
                }
            }
        } catch (e: Exception) {
            TranscriptionResult.Failure("No se pudo transcribir: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    private fun writeBody(out: OutputStream, file: File, boundary: String) {
        out.use { stream ->
            val newline = "\r\n"
            val head = buildString {
                append("--").append(boundary).append(newline)
                append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.name).append("\"").append(newline)
                append("Content-Type: audio/wav").append(newline).append(newline)
            }
            stream.write(head.toByteArray())
            file.inputStream().use { it.copyTo(stream) }
            val extra = buildString {
                append(newline)
                if (settings.model.isNotBlank()) {
                    append("--").append(boundary).append(newline)
                    append("Content-Disposition: form-data; name=\"model\"").append(newline)
                    append(newline).append(settings.model).append(newline)
                }
                if (settings.language.isNotBlank()) {
                    append("--").append(boundary).append(newline)
                    append("Content-Disposition: form-data; name=\"language\"").append(newline)
                    append(newline).append(settings.language).append(newline)
                }
                append("--").append(boundary).append(newline)
                append("Content-Disposition: form-data; name=\"response_format\"").append(newline)
                append(newline).append("verbose_json").append(newline)
                append("--").append(boundary).append("--").append(newline)
            }
            stream.write(extra.toByteArray())
            stream.flush()
        }
    }
}
