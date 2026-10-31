package com.vidcam.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

/**
 * Graba una toma de voz en off a 48 kHz mono, sin procesar.
 *
 * Se elige la fuente `UNPROCESSED` (que no aplica AGC ni supresores del sistema,
 * así la toma queda pareja y la limpieza la decide RNNoise) y se cae a
 * `VOICE_RECOGNITION` o `MIC` en los equipos que no la exponen.
 *
 * El audio se escribe primero en crudo y al terminar se envuelve en un WAV: así
 * no hay que reescribir la cabecera en cada bloque.
 */
class VoiceoverRecorder(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = VoiceoverProcessor.SAMPLE_RATE
        private const val READ_MS = 50
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private var rawFile: File? = null
    private var wavFile: File? = null
    private var stream: FileOutputStream? = null

    @Volatile
    private var running = false

    var isRecording: Boolean = false
        private set

    /**
     * Arranca una toma que se escribirá en [wav]. [onLevel] recibe el nivel
     * 0..1 en el hilo principal y [onFinished] la ruta final, o `null` si no se
     * pudo grabar nada.
     */
    fun start(
        wav: File,
        onLevel: (Float) -> Unit,
        onFinished: (File?) -> Unit,
    ): Boolean {
        if (running) return false
        if (!hasMicrophonePermission()) return false
        val audioRecord = openRecorder() ?: return false

        val raw = File(wav.parentFile, "${wav.nameWithoutExtension}.pcm")
        raw.parentFile?.mkdirs()
        raw.delete()
        rawFile = raw
        wavFile = wav
        stream = try {
            FileOutputStream(raw)
        } catch (_: Exception) {
            audioRecord.release()
            return false
        }
        record = audioRecord
        running = true
        isRecording = true

        thread = Thread({
            val buffer = ShortArray(SAMPLE_RATE / 1000 * READ_MS)
            var lastLevelPostMs = 0L
            try {
                audioRecord.startRecording()
                while (running) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read <= 0) {
                        if (!running) break
                        continue
                    }
                    stream?.write(WavFile.toBytes(buffer, read))
                    val now = System.currentTimeMillis()
                    if (now - lastLevelPostMs >= 100L) {
                        lastLevelPostMs = now
                        val level = PcmDsp.level(PcmDsp.rms(buffer, 0, read))
                        mainHandler.post { onLevel(level) }
                    }
                }
            } catch (_: Exception) {
                // Si la lectura falla, se cierra igual y se entrega lo grabado.
            } finally {
                finish(audioRecord, onFinished)
            }
        }, "vidcam-voiceover").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        return true
    }

    /** Pide detener la toma. El cierre y el WAV final llegan por [onFinished]. */
    fun stop() {
        if (!running) return
        running = false
        val active = record
        try {
            active?.stop()
        } catch (_: Exception) {
            // Ya estaba detenido: el hilo cierra igual.
        }
        // Espera corta para que el hilo cierre el archivo antes de que quien
        // llama quiera usarlo (por ejemplo, para procesarlo).
        try {
            thread?.join(1500)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun finish(audioRecord: AudioRecord, onFinished: (File?) -> Unit) {
        running = false
        isRecording = false
        try {
            audioRecord.stop()
        } catch (_: Exception) {
            // Sin grabación activa.
        }
        audioRecord.release()
        record = null
        try {
            stream?.flush()
            stream?.close()
        } catch (_: Exception) {
            // Se entrega lo que se haya podido escribir.
        }
        stream = null

        val raw = rawFile
        val target = wavFile
        rawFile = null
        wavFile = null
        var result: File? = null
        if (raw != null && target != null) {
            val pcm = try {
                raw.readBytes()
            } catch (_: Exception) {
                null
            }
            if (pcm != null && pcm.size >= 2) {
                val samples = ShortArray(pcm.size / 2)
                for (index in samples.indices) {
                    val low = pcm[index * 2].toInt() and 0xff
                    val high = pcm[index * 2 + 1].toInt()
                    samples[index] = ((high shl 8) or low).toShort()
                }
                if (WavFile.write(target, samples, SAMPLE_RATE)) result = target
            }
            raw.delete()
        }
        mainHandler.post { onFinished(result) }
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun openRecorder(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return null
        val bufferBytes = maxOf(minBuffer, SAMPLE_RATE / 10 * 2 * 4)
        val sources = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )
        for (source in sources) {
            val candidate = try {
                AudioRecord(
                    source,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferBytes,
                )
            } catch (_: Exception) {
                null
            } ?: continue
            if (candidate.state == AudioRecord.STATE_INITIALIZED) return candidate
            candidate.release()
        }
        return null
    }
}
