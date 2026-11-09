package com.vidcam.app.audio

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * Modulación de tono (pitch shifting) sobre audio PCM 16-bit mono a 48 kHz
 * manteniendo exactamente la duración del archivo.
 *
 * Emplea un algoritmo de doble retardo modulado con ventana de Hann/triangular
 * continua (Overlap-Add granular de fase cruzada), libre de clics y apto para
 * ejecución en tiempo real en CPU o en pruebas unitarias JVM sin dependencias
 * nativas.
 */
object VoicePitchShifter {

    /**
     * Calcula la relación de frecuencia correspondiente a un desplazamiento en
     * semitonos (-12..+12) y cents (-100..+100).
     */
    fun pitchRatio(semitones: Int, cents: Int = 0): Float {
        val totalCents = semitones * 100.0 + cents
        return 2.0.pow(totalCents / 1200.0).toFloat()
    }

    /**
     * Modula el tono de [samples] según [semitones] y [cents] manteniendo la
     * duración exacta. Retorna un nuevo [ShortArray].
     */
    fun shift(
        samples: ShortArray,
        sampleRate: Int = 48_000,
        semitones: Int,
        cents: Int = 0,
        windowSize: Int = 2048,
    ): ShortArray {
        if (samples.isEmpty()) return ShortArray(0)
        if (semitones == 0 && cents == 0) return samples.copyOf()
        val ratio = pitchRatio(semitones, cents)
        return shiftByRatio(samples, ratio, windowSize)
    }

    /**
     * Modula el tono según la razón multiplicativa [ratio] (2.0 = octava arriba,
     * 0.5 = octava abajo).
     */
    fun shiftByRatio(
        samples: ShortArray,
        ratio: Float,
        windowSize: Int = 2048,
    ): ShortArray {
        if (samples.isEmpty()) return ShortArray(0)
        if (ratio == 1f) return samples.copyOf()

        val output = ShortArray(samples.size)
        val bufferSize = windowSize.coerceIn(512, 8192)
        val delayBuffer = FloatArray(bufferSize)
        var writeIndex = 0

        val rate = 1.0 - ratio.toDouble()
        var phase = 0.0

        for (i in samples.indices) {
            val inputSample = samples[i].toFloat()
            delayBuffer[writeIndex] = inputSample

            var p1 = phase
            while (p1 < 0.0) p1 += bufferSize
            while (p1 >= bufferSize) p1 -= bufferSize

            var p2 = p1 + bufferSize * 0.5
            while (p2 >= bufferSize) p2 -= bufferSize

            // Ventana triangular que suma exactamente 1.0 en todo momento
            val norm1 = p1 / bufferSize
            val w1 = (1.0 - 2.0 * abs(norm1 - 0.5)).toFloat().coerceIn(0f, 1f)
            val w2 = 1f - w1

            val s1 = readInterpolated(delayBuffer, writeIndex, p1)
            val s2 = readInterpolated(delayBuffer, writeIndex, p2)

            val mixed = s1 * w1 + s2 * w2
            output[i] = mixed.toInt().coerceIn(-32768, 32767).toShort()

            writeIndex = (writeIndex + 1) % bufferSize
            phase += rate
            while (phase < 0.0) phase += bufferSize
            while (phase >= bufferSize) phase -= bufferSize
        }

        return output
    }

    private fun readInterpolated(buffer: FloatArray, writeIndex: Int, delay: Double): Float {
        val size = buffer.size
        var readPos = writeIndex.toDouble() - delay
        while (readPos < 0.0) readPos += size
        while (readPos >= size) readPos -= size

        val idx0 = floor(readPos).toInt() % size
        val idx1 = (idx0 + 1) % size
        val frac = (readPos - floor(readPos)).toFloat()

        return buffer[idx0] * (1f - frac) + buffer[idx1] * frac
    }
}
