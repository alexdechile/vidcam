package com.vidcam.app.audio

/**
 * Envoltorio del puente JNI sobre RNNoise (aislación de entorno de la voz).
 *
 * La librería nativa se compila desde `app/src/main/cpp` con CMake. Si el
 * dispositivo no la puede cargar (ABI no incluida, tests unitarios en la JVM),
 * [isAvailable] queda en `false` y la limpieza de voz se omite con elegancia en
 * vez de tumbar la grabación.
 */
object Rnnoise {

    private const val LIBRARY = "rnnoise_jni"

    /** Tamaño de fotograma de RNNoise (480 muestras = 10 ms a 48 kHz). */
    private const val DEFAULT_FRAME_SIZE = 480

    val isAvailable: Boolean = runCatching { System.loadLibrary(LIBRARY) }.isSuccess

    val frameSize: Int = if (isAvailable) nativeFrameSize() else DEFAULT_FRAME_SIZE

    /** Crea un estado de RNNoise; devuelve `0` si no se pudo reservar. */
    fun create(): Long = if (isAvailable) nativeCreate() else 0L

    /**
     * Procesa un fotograma de [count] muestras PCM 16 bits. [output] recibe el
     * audio limpio en la escala de int16. Devuelve la probabilidad de voz (0..1).
     */
    fun process(handle: Long, input: ShortArray, output: FloatArray, count: Int): Float =
        if (isAvailable && handle != 0L) nativeProcess(handle, input, output, count) else 0f

    fun destroy(handle: Long) {
        if (isAvailable && handle != 0L) nativeDestroy(handle)
    }

    private external fun nativeFrameSize(): Int

    private external fun nativeCreate(): Long

    private external fun nativeDestroy(handle: Long)

    private external fun nativeProcess(
        handle: Long,
        input: ShortArray,
        output: FloatArray,
        count: Int,
    ): Float
}
