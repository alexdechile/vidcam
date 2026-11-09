package com.vidcam.app.data

import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerTransitionPreset
import com.vidcam.app.model.PngAnimationPreset
import com.vidcam.app.model.scheme.FactorySchemes
import com.vidcam.app.model.scheme.SchemeBackground
import com.vidcam.app.model.scheme.SchemeSlot
import com.vidcam.app.model.scheme.SlotKind
import com.vidcam.app.model.scheme.UsageScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SchemeStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val store: SchemeStore by lazy { SchemeStore(temp.root.resolve("templates")) }

    @Test
    fun list_includesFactorySchemes() {
        val list = store.list()
        assertTrue(list.size >= FactorySchemes.ALL.size)
        val cascade = list.firstOrNull { it.id == FactorySchemes.CASCADE_5_STAGES_15S.id }
        assertNotNull(cascade)
        assertFalse(cascade!!.isCustom)
        assertEquals(15, cascade.scheme.totalDurationSec)
        assertEquals(5, cascade.scheme.stagesCount)
    }

    @Test
    fun saveAndLoad_customScheme() {
        val custom = SavedScheme(
            id = "custom_test_1",
            name = "Mi Plantilla Favorita",
            createdAtMs = 1000L,
            updatedAtMs = 1000L,
            isCustom = true,
            scheme = UsageScheme(
                id = "custom_test_1",
                name = "Mi Plantilla Favorita",
                totalDurationSec = 20,
                stagesCount = 4,
                background = SchemeBackground(
                    kenBurns = KenBurnsMode.ALEJAR,
                    stageStart = 1,
                    stageEnd = 4,
                ),
                slots = listOf(
                    SchemeSlot(
                        id = "slot_1",
                        kind = SlotKind.IMAGE,
                        stageStart = 1,
                        stageEnd = 2,
                        transition = LayerTransitionPreset.POP,
                        animation = PngAnimationPreset.PULSO,
                    )
                ),
            ),
        )

        val savedOk = store.save(custom)
        assertTrue(savedOk)

        val loaded = store.load("custom_test_1")
        assertNotNull(loaded)
        assertEquals("Mi Plantilla Favorita", loaded!!.name)
        assertTrue(loaded.isCustom)
        assertEquals(20, loaded.scheme.totalDurationSec)
        assertEquals(1, loaded.scheme.slots.size)

        // Ver que figure en listCustom()
        val customList = store.listCustom()
        assertEquals(1, customList.size)
        assertEquals("custom_test_1", customList[0].id)
    }

    @Test
    fun delete_removesCustomScheme_butRejectsFactory() {
        // No se puede borrar uno de fábrica
        val factoryDelete = store.delete(FactorySchemes.CASCADE_5_STAGES_15S.id)
        assertFalse(factoryDelete)

        // Guardar y borrar uno personalizado
        val custom = SavedScheme(
            id = "to_delete",
            name = "Temporal",
            createdAtMs = 2000L,
            updatedAtMs = 2000L,
            scheme = UsageScheme(id = "to_delete", name = "Temporal"),
        )
        store.save(custom)
        assertNotNull(store.load("to_delete"))

        val deleted = store.delete("to_delete")
        assertTrue(deleted)
        assertNull(store.load("to_delete"))
    }

    @Test
    fun exportAndImportJson() {
        val scheme = FactorySchemes.CASCADE_5_STAGES_15S
        val jsonString = store.exportToJson(scheme)
        assertTrue(jsonString.contains("factory_cascade_15s"))

        val imported = store.importFromJson(jsonString)
        assertNotNull(imported)
        assertEquals(scheme.id, imported!!.id)
        assertEquals(scheme.slots.size, imported.slots.size)
    }
}
