package com.vidcam.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Un tramo de transcripción, en milisegundos. */
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/**
 * Lectura de respuestas de transcripción.
 *
 * Se aceptan los formatos que devuelven los servicios habituales:
 * - Whisper con `response_format=verbose_json` → `segments[]` con `start`/`end`.
 * - Algunos servidores devuelven `chunks[]` con la misma forma.
 * - Solo texto → se reparte en tramos proporcionales a la duración de la toma.
 *
 * Sin dependencias de Android para poder probarlo con tests unitarios.
 */
object TranscriptParser {

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    /** Máximo de palabras por subtítulo cuando el servicio no da tiempos. */
    private const val MAX_WORDS_PER_CHUNK = 8

    /** Máximo de caracteres por subtítulo en el reparto sin tiempos. */
    private const val MAX_CHARS_PER_CHUNK = 48

    /**
     * Convierte el cuerpo de la respuesta en tramos. [durationMs] es la duración
     * de la toma, que se usa solo cuando el servicio no entrega tiempos.
     */
    fun parse(body: String, durationMs: Long): List<TranscriptSegment> {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull()
            ?: return fallback(body.trim(), durationMs)
        if (root !is JsonObject) return fallback(body.trim(), durationMs)

        timedSegments(root["segments"])?.let { return it }
        timedSegments(root["chunks"])?.let { return it }

        val text = root["text"]?.jsonPrimitive?.content?.trim().orEmpty()
        return fallback(text, durationMs)
    }

    private fun timedSegments(element: kotlinx.serialization.json.JsonElement?): List<TranscriptSegment>? {
        if (element == null) return null
        val array = runCatching { element.jsonArray }.getOrNull() ?: return null
        val segments = array.mapNotNull { item ->
            val obj = runCatching { item.jsonObject }.getOrNull() ?: return@mapNotNull null
            val text = obj["text"]?.jsonPrimitive?.content?.trim().orEmpty()
            val start = obj.seconds("start") ?: return@mapNotNull null
            val end = obj.seconds("end") ?: return@mapNotNull null
            if (text.isBlank() || end <= start) null else TranscriptSegment(start, end, text)
        }
        return segments.takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.seconds(key: String): Long? {
        val primitive = this[key]?.jsonPrimitive ?: return null
        val value = primitive.content.toDoubleOrNull() ?: return null
        return (value * 1000.0).toLong()
    }

    /** Reparte un texto sin tiempos en tramos de duración proporcional. */
    fun fallback(text: String, durationMs: Long): List<TranscriptSegment> {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty() || durationMs <= 0L) return emptyList()
        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (word in words) {
            val candidate = (current + word).joinToString(" ")
            if (current.isNotEmpty() &&
                (current.size >= MAX_WORDS_PER_CHUNK || candidate.length > MAX_CHARS_PER_CHUNK)
            ) {
                chunks += current
                current = mutableListOf()
            }
            current.add(word)
        }
        if (current.isNotEmpty()) chunks += current

        val totalWords = words.size
        var wordsBefore = 0
        val result = mutableListOf<TranscriptSegment>()
        for (chunk in chunks) {
            val start = durationMs * wordsBefore / totalWords
            wordsBefore += chunk.size
            val end = durationMs * wordsBefore / totalWords
            result += TranscriptSegment(
                startMs = start,
                endMs = maxOf(end, start + 1L),
                text = chunk.joinToString(" "),
            )
        }
        return result
    }
}
