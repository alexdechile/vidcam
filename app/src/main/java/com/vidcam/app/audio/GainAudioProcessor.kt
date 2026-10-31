package com.vidcam.app.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Aplica una ganancia constante a una pista de audio durante la exportación.
 *
 * Es la forma de dar volumen por pista en Media3 1.5: `EditedMediaItem` no
 * expone volumen y el mezclador de secuencias es interno, así que cada pista
 * entra a la mezcla ya escalada (voz, audio original y música).
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor(private val gain: Float) : BaseAudioProcessor() {

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        require(gain > 0f) { "La ganancia debe ser positiva" }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        outputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 2) {
                    val scaled = (inputBuffer.short * gain).toInt().coerceIn(-32768, 32767)
                    outputBuffer.putShort(scaled.toShort())
                }
            }

            C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 4) {
                    outputBuffer.putFloat((inputBuffer.float * gain).coerceIn(-1f, 1f))
                }
            }

            else -> error("Formato de audio no soportado: ${inputAudioFormat.encoding}")
        }
        // Restos impares (no deberían existir): se copian tal cual.
        while (inputBuffer.hasRemaining()) outputBuffer.put(inputBuffer.get())
        outputBuffer.flip()
    }
}
