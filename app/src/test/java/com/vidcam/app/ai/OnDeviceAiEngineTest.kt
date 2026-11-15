package com.vidcam.app.ai

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnDeviceAiEngineTest {

    private class FakeBackend(
        private val reply: (String) -> String = { "Titular: $it" },
    ) : LlmBackend {
        var closed = false
        var calls = 0

        override fun generateResponse(prompt: String): String {
            calls++
            return reply(prompt)
        }

        override fun close() {
            closed = true
        }
    }

    @Test
    fun `engine stays idle until first generate call and reuses loaded backend`() = runTest {
        val created = mutableListOf<Pair<String, FakeBackend>>()
        val engine = OnDeviceAiEngine(UnconfinedTestDispatcher(testScheduler)) { path ->
            FakeBackend().also { created.add(path to it) }
        }

        assertFalse(engine.isLoaded.value)
        assertEquals(AiEngineState.IDLE, engine.state.value)

        val first = engine.generate("/models/gemma.task", "vlogs")
        assertTrue(first.isSuccess)
        assertEquals("Titular: vlogs", first.getOrNull())
        assertTrue(engine.isLoaded.value)
        assertEquals(AiEngineState.READY, engine.state.value)
        assertEquals(1, created.size)

        val second = engine.generate("/models/gemma.task", "cocina")
        assertTrue(second.isSuccess)
        assertEquals(1, created.size)
        assertEquals(2, created[0].second.calls)
    }

    @Test
    fun `release closes backend and resets state to idle`() = runTest {
        val backend = FakeBackend()
        val engine = OnDeviceAiEngine(UnconfinedTestDispatcher(testScheduler)) { backend }

        engine.generate("/models/gemma.task", "hola")
        assertTrue(engine.isLoaded.value)
        assertFalse(backend.closed)

        engine.release()
        assertTrue(backend.closed)
        assertFalse(engine.isLoaded.value)
        assertEquals(AiEngineState.IDLE, engine.state.value)
    }

    @Test
    fun `switching model path closes previous backend before loading new one`() = runTest {
        val backends = mutableListOf<FakeBackend>()
        val engine = OnDeviceAiEngine(UnconfinedTestDispatcher(testScheduler)) {
            FakeBackend().also { backends.add(it) }
        }

        engine.generate("/models/a.task", "uno")
        engine.generate("/models/b.litertlm", "dos")

        assertEquals(2, backends.size)
        assertTrue(backends[0].closed)
        assertFalse(backends[1].closed)
    }

    @Test
    fun `abi or native init failure returns failure without crashing`() = runTest {
        val engine = OnDeviceAiEngine(UnconfinedTestDispatcher(testScheduler)) {
            throw UnsatisfiedLinkError("x86_64 not supported")
        }

        val result = engine.generate("/models/gemma.task", "hola")
        assertTrue(result.isFailure)
        assertFalse(engine.isLoaded.value)
        assertEquals(AiEngineState.ERROR, engine.state.value)
    }
}
