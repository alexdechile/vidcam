package com.vidcam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class LocalAiModelStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newStore(): LocalAiModelStore {
        val root = tempFolder.newFolder()
        return LocalAiModelStore(
            modelsDir = File(root, "models"),
            prefsFile = File(root, "ai_active_model.path"),
        )
    }

    @Test
    fun `isSupportedModelFileName accepts task and litertlm case-insensitively`() {
        assertTrue(LocalAiModelStore.isSupportedModelFileName("gemma-2b.task"))
        assertTrue(LocalAiModelStore.isSupportedModelFileName("Gemma3-1B.LITERTLM"))
        assertFalse(LocalAiModelStore.isSupportedModelFileName("weights.bin"))
        assertFalse(LocalAiModelStore.isSupportedModelFileName("no_extension"))
        assertFalse(LocalAiModelStore.isSupportedModelFileName("   "))
    }

    @Test
    fun `importFromInputStream persists valid task model and survives new store instance`() {
        val root = tempFolder.newFolder()
        val modelsDir = File(root, "models")
        val prefsFile = File(root, "ai_active_model.path")
        val store1 = LocalAiModelStore(modelsDir, prefsFile)

        val payload = "dummy-gemma-weights".toByteArray()
        val result = store1.importFromInputStream("gemma3-1b.task", ByteArrayInputStream(payload))

        assertTrue(result.isSuccess)
        val registered = result.getOrThrow()
        assertEquals("gemma3-1b.task", registered.name)
        assertEquals(payload.size.toLong(), registered.sizeBytes)
        assertTrue(File(registered.path).exists())

        val store2 = LocalAiModelStore(modelsDir, prefsFile)
        val reloaded = store2.activeModel()
        assertNotNull(reloaded)
        assertEquals(registered.path, reloaded?.path)
    }

    @Test
    fun `importFromInputStream rejects unsupported extension and empty stream`() {
        val store = newStore()

        val badExt = store.importFromInputStream("model.gguf", ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        assertTrue(badExt.isFailure)
        assertNull(store.activeModel())

        val emptyStream = store.importFromInputStream("empty.litertlm", ByteArrayInputStream(ByteArray(0)))
        assertTrue(emptyStream.isFailure)
        assertNull(store.activeModel())
    }

    @Test
    fun `activeModel returns null when file is deleted externally and clearActiveModel cleans up`() {
        val store = newStore()
        val registered = store.importFromInputStream(
            "model.litertlm",
            ByteArrayInputStream("abc".toByteArray()),
        ).getOrThrow()

        assertTrue(File(registered.path).delete())
        assertNull(store.activeModel())

        val second = store.importFromInputStream(
            "second.task",
            ByteArrayInputStream("xyz".toByteArray()),
        ).getOrThrow()
        assertNotNull(store.activeModel())

        store.clearActiveModel()
        assertNull(store.activeModel())
        assertFalse(File(second.path).exists())
    }
}
