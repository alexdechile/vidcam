package com.vidcam.app.model.scheme

import com.vidcam.app.model.BackgroundLayer
import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.LayerTransitionPreset
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.PngAnimationPreset
import com.vidcam.app.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchemeBindingTest {

    private val testScheme = UsageScheme(
        id = "cascade-15s",
        name = "5 Etapas en Cascada 15s",
        totalDurationSec = 15,
        stagesCount = 5,
        background = SchemeBackground(
            kenBurns = KenBurnsMode.PANEO,
            label = "Fondo Paneo",
        ),
        slots = listOf(
            SchemeSlot(
                id = "img1",
                kind = SlotKind.IMAGE,
                label = "Gancho",
                stageStart = 1,
                stageEnd = 2,
                transition = LayerTransitionPreset.POP,
                animation = PngAnimationPreset.FLOTAR,
            ),
            SchemeSlot(
                id = "img2",
                kind = SlotKind.IMAGE,
                label = "Desarrollo",
                stageStart = 2,
                stageEnd = 4,
                transition = LayerTransitionPreset.DESLIZAR,
                animation = PngAnimationPreset.PULSO,
            ),
            SchemeSlot(
                id = "txt1",
                kind = SlotKind.TEXT,
                label = "Titular",
                stageStart = 1,
                stageEnd = 3,
                text = "Oferta Imperdible",
            ),
        ),
    )

    @Test
    fun prepareInitialBinding_emptyProject_createsEmptyBindings() {
        val binding = SchemeTiming.prepareInitialBinding(testScheme, Project())

        assertEquals("Fondo Paneo", binding.background.label)
        assertNull(binding.background.uri)
        assertFalse(binding.background.omitted)

        val slotImg1 = binding.getSlot("img1")
        assertEquals("Gancho", slotImg1.label)
        assertNull(slotImg1.uri)
        assertFalse(slotImg1.omitted)

        val slotTxt = binding.getSlot("txt1")
        assertEquals("Oferta Imperdible", slotTxt.text)
    }

    @Test
    fun prepareInitialBinding_existingAssets_autoMapsInOrder() {
        val currentProject = Project(
            backgrounds = listOf(
                BackgroundLayer(
                    id = "bg-existing",
                    uri = "content://media/existing_bg.jpg",
                    label = "Mi Fondo Paisaje",
                ),
            ),
            layers = listOf(
                OverlayLayer(
                    id = "l1",
                    kind = LayerKind.PNG,
                    uri = "content://media/logo.png",
                    label = "Logo Empresa",
                ),
                OverlayLayer(
                    id = "l2",
                    kind = LayerKind.PNG,
                    uri = "content://media/product.png",
                    label = "Foto Producto",
                ),
                OverlayLayer(
                    id = "l3",
                    kind = LayerKind.TEXT,
                    text = "Texto Existente en Proyecto",
                    label = "Rótulo 1",
                ),
            ),
        )

        val binding = SchemeTiming.prepareInitialBinding(testScheme, currentProject)

        // Fondo mapeado
        assertEquals("content://media/existing_bg.jpg", binding.background.uri)

        // PNGs mapeados en orden
        val b1 = binding.getSlot("img1")
        assertEquals("content://media/logo.png", b1.uri)

        val b2 = binding.getSlot("img2")
        assertEquals("content://media/product.png", b2.uri)

        // Texto mapeado desde la capa existente
        val bTxt = binding.getSlot("txt1")
        assertEquals("Texto Existente en Proyecto", bTxt.text)
    }

    @Test
    fun withBatchImages_assignsSequentiallyToImageSlots() {
        val initial = SchemeTiming.prepareInitialBinding(testScheme, Project())
        val batchUris = listOf(
            "content://batch/img_A.png",
            "content://batch/img_B.png",
        )

        val updated = SchemeTiming.withBatchImages(initial, batchUris)

        assertEquals("content://batch/img_A.png", updated.getSlot("img1").uri)
        assertEquals("content://batch/img_B.png", updated.getSlot("img2").uri)
        // El slot de texto no se toca
        assertEquals("Oferta Imperdible", updated.getSlot("txt1").text)
    }

    @Test
    fun applyBindingToProject_createsConfiguredLayersAndBackground() {
        val binding = SchemeBinding(
            scheme = testScheme,
            background = BackgroundBinding(
                uri = "content://media/bg_neon.png",
                label = "Neon Fondo",
            ),
            slots = mapOf(
                "img1" to SlotBinding(
                    slotId = "img1",
                    uri = "content://media/person.png",
                    label = "Actor Principal",
                ),
                "img2" to SlotBinding(
                    slotId = "img2",
                    omitted = true, // Omitido por el usuario
                ),
                "txt1" to SlotBinding(
                    slotId = "txt1",
                    text = "Gran Descuento",
                ),
            ),
            openVoiceoverOnApply = true,
        )

        val appliedProject = SchemeTiming.applyBindingToProject(binding, Project())

        // 1. Duración objetivo
        assertEquals(15_000L, appliedProject.targetDurationMs)

        // 2. Fondo
        assertEquals(1, appliedProject.backgrounds.size)
        val bg = appliedProject.backgrounds.first()
        assertEquals("content://media/bg_neon.png", bg.uri)
        assertEquals(KenBurnsMode.PANEO, bg.kenBurns)

        // 3. Capas: img1 y txt1 (img2 fue omitida)
        assertEquals(2, appliedProject.layers.size)

        val img1Layer = appliedProject.layers.first { it.kind == LayerKind.PNG }
        assertEquals("content://media/person.png", img1Layer.uri)
        assertEquals(0L, img1Layer.startMs)
        assertEquals(6_000L, img1Layer.endMs)
        assertEquals(LayerTransitionPreset.POP, img1Layer.transition)
        assertEquals(PngAnimationPreset.FLOTAR, img1Layer.animation)

        val txtLayer = appliedProject.layers.first { it.kind == LayerKind.TEXT }
        assertEquals("Gran Descuento", txtLayer.text)
        assertEquals(0L, txtLayer.startMs)
        assertEquals(9_000L, txtLayer.endMs) // 3 etapas de 3000ms = 9000ms
    }
}
