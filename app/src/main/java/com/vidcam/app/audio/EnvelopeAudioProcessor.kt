package com.vidcam.app.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.vidcam.app.model.AudioMixMath
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Aplica una ganancia variable en el tiempo a una pista de audio: es lo que
 * permite fundidos de entrada/salida y *ducking* de la música bajo la voz.
 *
 * La ganancia se evalúa cada [AudioMixMath.BLOCK_MS] de la pista (no por
 * muestra) y se mantiene constante dentro del bloque: suficiente para oído y
 * mucho más barato que recalcular una curva por muestra.
 */
@OptIn(UnstableApi::class)
class EnvelopeAudioProcessor(
    private val gainAtMs: (Long) -> Float,
) : BaseAudioProcessor() {

    private var processedFrames = 0L
    private var blockFrames = 1
    private var currentGain = 1f
    private var framesInBlock = 0

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        processedFrames = 0
        framesInBlock = 0
        blockFrames = (inputAudioFormat.sampleRate * AudioMixMath.BLOCK_MS / 1000L)
            .toInt()
            .coerceAtLeast(1)
        currentGain = gainAtMs(0L).coerceIn(0f, 1f)
        return inputAudioFormat
    }

    override fun onReset() {
        processedFrames = 0
        framesInBlock = 0
        currentGain = 1f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        outputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 2) {
                    if (framesInBlock == 0) {
                        currentGain = gainAt(processedFrames, inputAudioFormat.sampleRate)
                    }
                    val scaled = (inputBuffer.short * currentGain).toInt().coerceIn(-32768, 32767)
                    outputBuffer.putShort(scaled.toShort())
                    advanceFrames(channels)
                }
            }

            C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 4) {
                    if (framesInBlock == 0) {
                        currentGain = gainAt(processedFrames, inputAudioFormat.sampleRate)
                    }
                    outputBuffer.putFloat((inputBuffer.float * currentGain).coerceIn(-1f, 1f))
                    advanceFrames(channels)
                }
            }

            else -> error("Formato de audio no soportado: ${inputAudioFormat.encoding}")
        }
        while (inputBuffer.hasRemaining()) outputBuffer.put(inputBuffer.get())
        outputBuffer.flip()
    }

    private fun gainAt(frames: Long, sampleRate: Int): Float =
        gainAtMs(frames * 1000L / sampleRate.coerceAtLeast(1)).coerceIn(0f, 1f)

    private fun advanceFrames(channels: Int) {
        framesInBlock++
        if (framesInBlock >= blockFrames) framesInBlock = 0
        processedFrames += channels
    }
}
