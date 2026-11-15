package com.vidcam.app.ai

import android.content.Context
import android.os.Build
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class AiEngineState {
    IDLE,
    LOADING,
    READY,
    GENERATING,
    ERROR,
}

/**
 * Abstraction over the native MediaPipe [LlmInference] engine so lifecycle and
 * error-handling logic can be unit-tested on the JVM without native JNI binaries.
 */
interface LlmBackend {
    fun generateResponse(prompt: String): String
    fun close()
}

private class MediaPipeLlmBackend(
    private val inference: LlmInference,
) : LlmBackend {
    override fun generateResponse(prompt: String): String = inference.generateResponse(prompt)

    override fun close() {
        inference.close()
    }

    companion object {
        fun isArm64Supported(supportedAbis: Array<String> = Build.SUPPORTED_ABIS): Boolean =
            supportedAbis.any { it == "arm64-v8a" }

        fun factory(context: Context): (String) -> LlmBackend {
            val appContext = context.applicationContext
            return { modelPath ->
                if (!isArm64Supported()) {
                    throw IllegalStateException(
                        "La IA local requiere arquitectura arm64-v8a (detectada: ${Build.SUPPORTED_ABIS.joinToString()})",
                    )
                }
                try {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(512)
                        .build()
                    MediaPipeLlmBackend(LlmInference.createFromOptions(appContext, options))
                } catch (t: Throwable) {
                    throw IllegalStateException(
                        "No se pudo inicializar el modelo local: ${t.message ?: t::class.java.simpleName}",
                        t,
                    )
                }
            }
        }
    }
}

/**
 * On-device LLM inference wrapper that loads the model lazily on [Dispatchers.IO],
 * protects against incompatible ABIs/native errors, and exposes [release] to free
 * RAM before video/frame exports.
 */
class OnDeviceAiEngine(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val backendFactory: (String) -> LlmBackend,
) {
    constructor(context: Context) : this(
        ioDispatcher = Dispatchers.IO,
        backendFactory = MediaPipeLlmBackend.factory(context),
    )

    private val mutex = Mutex()
    private var backend: LlmBackend? = null
    private var loadedModelPath: String? = null

    private val _state = MutableStateFlow(AiEngineState.IDLE)
    val state: StateFlow<AiEngineState> = _state.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /**
     * Ensures the model at [modelPath] is loaded and generates a response for [prompt].
     */
    suspend fun generate(modelPath: String, prompt: String): Result<String> {
        if (modelPath.isBlank()) {
            return Result.failure(IllegalArgumentException("No hay ningún modelo local configurado"))
        }
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Escribe una instrucción para generar texto"))
        }

        return withContext(ioDispatcher) {
            mutex.withLock {
                if (backend == null || loadedModelPath != modelPath) {
                    releaseLocked()
                    _state.value = AiEngineState.LOADING
                    val created = try {
                        backendFactory(modelPath)
                    } catch (t: Throwable) {
                        _state.value = AiEngineState.ERROR
                        _isLoaded.value = false
                        return@withLock Result.failure(
                            if (t is Exception) t else IllegalStateException(t.message, t),
                        )
                    }
                    backend = created
                    loadedModelPath = modelPath
                    _isLoaded.value = true
                }

                _state.value = AiEngineState.GENERATING
                try {
                    val response = backend!!.generateResponse(prompt).trim()
                    _state.value = AiEngineState.READY
                    Result.success(response)
                } catch (t: Throwable) {
                    _state.value = AiEngineState.ERROR
                    Result.failure(
                        if (t is Exception) t else IllegalStateException(t.message, t),
                    )
                }
            }
        }
    }

    /**
     * Releases the loaded model from memory so RAM is available for MP4/PNG export.
     */
    suspend fun release() {
        withContext(NonCancellable + ioDispatcher) {
            mutex.withLock {
                releaseLocked()
            }
        }
    }

    /**
     * Best-effort synchronous release for ViewModel `onCleared()`.
     */
    fun releaseNow() {
        runCatching { backend?.close() }
        backend = null
        loadedModelPath = null
        _isLoaded.value = false
        _state.value = AiEngineState.IDLE
    }

    private fun releaseLocked() {
        runCatching { backend?.close() }
        backend = null
        loadedModelPath = null
        _isLoaded.value = false
        _state.value = AiEngineState.IDLE
    }
}
