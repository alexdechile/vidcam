package com.vidcam.app.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Audio PCM 16 bits de un canal leído de un WAV. */
class PcmAudio(val samples: ShortArray, val sampleRate: Int) {

    val durationMs: Long
        get() = if (sampleRate > 0) samples.size * 1000L / sampleRate else 0L
}

/**
 * Lectura y escritura de WAV PCM 16 bits mono, que es el formato en el que se
 * guardan las tomas de voz en off.
 *
 * No depende de Android, así que se prueba con tests unitarios en la JVM.
 */
object WavFile {

    const val HEADER_BYTES = 44
    const val BITS_PER_SAMPLE = 16

    /** Escribe [samples] completo. */
    fun write(file: File, samples: ShortArray, sampleRate: Int): Boolean =
        writeSegment(
            file = file,
            samples = samples,
            sampleRate = sampleRate,
            padSamples = 0,
            skipSamples = 0,
            takeSamples = samples.size,
        )

    /**
     * Escribe una copia del tramo `[skipSamples, skipSamples + takeSamples)`
     * precedido de [padSamples] muestras de silencio.
     *
     * La exportación lo usa para colocar la toma en su segundo de entrada
     * (relleno) o para adelantarla (recorte) sin tocar el archivo original.
     */
    fun writeSegment(
        file: File,
        samples: ShortArray,
        sampleRate: Int,
        padSamples: Int,
        skipSamples: Int,
        takeSamples: Int,
    ): Boolean {
        val skip = skipSamples.coerceIn(0, samples.size)
        val take = takeSamples.coerceAtMost(samples.size - skip).coerceAtLeast(0)
        val pad = padSamples.coerceAtLeast(0)
        val totalSamples = pad + take
        return try {
            file.parentFile?.mkdirs()
            RandomAccessFile(file, "rw").use { out ->
                out.setLength(0)
                out.write(header(totalSamples * 2L, sampleRate))
                // Silencio en bloques de 100 ms para no reservar el relleno entero.
                val silence = ByteArray(SAMPLE_RATE_BLOCK * 2)
                var remaining = pad
                while (remaining > 0) {
                    val block = minOf(remaining, SAMPLE_RATE_BLOCK)
                    out.write(silence, 0, block * 2)
                    remaining -= block
                }
                if (take > 0) {
                    val bytes = ByteArray(take * 2)
                    var index = 0
                    var offset = 0
                    while (index < take) {
                        val value = samples[skip + index].toInt()
                        bytes[offset] = (value and 0xff).toByte()
                        bytes[offset + 1] = ((value shr 8) and 0xff).toByte()
                        index++
                        offset += 2
                    }
                    out.write(bytes)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Duración en milisegundos leída de la cabecera, sin cargar el audio en
     * memoria. Devuelve 0 si el archivo no es un WAV PCM 16 bits mono.
     */
    fun durationMs(file: File): Long = try {
        RandomAccessFile(file, "r").use { input ->
            val length = input.length()
            if (length < HEADER_BYTES) {
                0L
            } else {
                val info = readInfo(input, length)
                if (info == null) 0L else info.dataBytes * 1000L / (info.sampleRate * 2L)
            }
        }
    } catch (_: Exception) {
        0L
    }

    /** Lee un WAV PCM 16 bits mono; `null` si no se puede interpretar. */
    fun read(file: File): PcmAudio? = try {
        RandomAccessFile(file, "r").use { input ->
            val length = input.length()
            val info = readInfo(input, length)
            if (info == null) {
                null
            } else {
                val count = (info.dataBytes / 2).toInt()
                val data = ByteArray(count * 2)
                input.seek(info.dataOffset)
                input.readFully(data)
                val samples = ShortArray(count)
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
                PcmAudio(samples, info.sampleRate)
            }
        }
    } catch (_: Exception) {
        null
    }

    private class Info(
        val sampleRate: Int,
        val dataOffset: Long,
        val dataBytes: Long,
    )

    /** Recorre las cabeceras RIFF y devuelve el tramo de datos PCM mono 16 bits. */
    private fun readInfo(input: RandomAccessFile, length: Long): Info? {
        if (length < HEADER_BYTES) return null
        val head = ByteArray(12)
        input.seek(0)
        input.readFully(head)
        if (String(head, 0, 4) != "RIFF" || String(head, 8, 4) != "WAVE") return null
        var sampleRate = 0
        var channels = 0
        var bits = 0
        var dataOffset = -1L
        var dataBytes = 0L
        var position = 12L
        while (position + 8 <= length) {
            input.seek(position)
            val chunk = ByteArray(8)
            input.readFully(chunk)
            val id = String(chunk, 0, 4)
            val size = ByteBuffer.wrap(chunk, 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
                .toLong() and 0xffffffffL
            if (id == "fmt " && size >= 16) {
                val fmt = ByteArray(size.toInt().coerceAtMost(64))
                input.readFully(fmt)
                val buffer = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                buffer.short // formato de audio (1 = PCM)
                channels = buffer.short.toInt()
                sampleRate = buffer.int
                buffer.int // bytes por segundo
                buffer.short // alineación de bloque
                bits = buffer.short.toInt()
            } else if (id == "data") {
                dataOffset = position + 8
                dataBytes = minOf(size, length - dataOffset)
            }
            position += 8 + size + (size and 1L)
        }
        if (dataOffset < 0 || channels != 1 || bits != BITS_PER_SAMPLE || sampleRate <= 0) {
            return null
        }
        return Info(sampleRate, dataOffset, dataBytes)
    }

    /** Convierte [count] muestras PCM a bytes little endian (como los guarda el WAV). */
    fun toBytes(samples: ShortArray, count: Int = samples.size): ByteArray {
        val size = count.coerceIn(0, samples.size)
        val bytes = ByteArray(size * 2)
        var index = 0
        var offset = 0
        while (index < size) {
            val value = samples[index].toInt()
            bytes[offset] = (value and 0xff).toByte()
            bytes[offset + 1] = ((value shr 8) and 0xff).toByte()
            index++
            offset += 2
        }
        return bytes
    }

    private fun header(dataBytes: Long, sampleRate: Int): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        val byteRate = sampleRate * 2
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt((36 + dataBytes).toInt())
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // mono
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(2) // bytes por muestra
        buffer.putShort(BITS_PER_SAMPLE.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataBytes.toInt())
        return buffer.array()
    }

    /** Bloque de silencio del relleno: 100 ms a 48 kHz. */
    private const val SAMPLE_RATE_BLOCK = 4800
}
