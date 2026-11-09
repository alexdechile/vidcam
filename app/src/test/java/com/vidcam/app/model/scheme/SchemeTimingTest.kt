package com.vidcam.app.model.scheme

import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerTransitionPreset
import com.vidcam.app.model.PngAnimationPreset
import com.vidcam.app.model.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SchemeTimingTest {

    @Test
    fun calculateIntervalMs_5Stages15Seconds_matchesCascadingRules() {
        val totalMs = 15_000L
        val stages = 5

        // Etapa 1 a 2: 0s a 6s
        val (s1Start, s1End) = SchemeTiming.calculateIntervalMs(1, 2, stages, totalMs)
        assertEquals(0L, s1Start)
        assertEquals(6_000L, s1End)

        // Etapa 2 a 4: 3s a 12s
        val (s2Start, s2End) = SchemeTiming.calculateIntervalMs(2, 4, stages, totalMs)
        assertEquals(3_000L, s2Start)
        assertEquals(12_000L, s2End)

        // Etapa 3 a 5: 6s a 15s
        val (s3Start, s3End) = SchemeTiming.calculateIntervalMs(3, 5, stages, totalMs)
        assertEquals(6_000L, s3Start)
        assertEquals(15_000L, s3End)

        // Fondo completo 1 a 5: 0s a 15s
        val (bgStart, bgEnd) = SchemeTiming.calculateIntervalMs(1, 5, stages, totalMs)
        assertEquals(0L, bgStart)
        assertEquals(15_000L, bgEnd)
    }

    @Test
    fun calculateIntervalMs_clampsOutOfRangeStages() {
        val totalMs = 10_000L
        val stages = 4 // 2500 ms per stage

        // stageStart < 1 se acota a 1; stageEnd > 4 se acota a 4
        val (start, end) = SchemeTiming.calculateIntervalMs(-2, 10, stages, totalMs)
        assertEquals(0L, start)
        assertEquals(10_000L, end)

        // Inversión: stageStart > stageEnd -> endStage se acota a startStage
        val (invStart, invEnd) = SchemeTiming.calculateIntervalMs(3, 1, stages, totalMs)
        assertEquals(5_000L, invStart)
        assertEquals(7_500L, invEnd)
    }

    @Test
    fun applyToProject_populatesLayersAndBackground() {
        val scheme = UsageScheme(
            id = "test-cascade",
            name = "Cascada 15s",
            totalDurationSec = 15,
            stagesCount = 5,
            background = SchemeBackground(
                kenBurns = KenBurnsMode.PANEO,
                stageStart = 1,
                stageEnd = 5,
            ),
            slots = listOf(
                SchemeSlot(
                    id = "img1",
                    kind = SlotKind.IMAGE,
                    label = "Intro",
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
                    id = "img3",
                    kind = SlotKind.IMAGE,
                    label = "Cierre",
                    stageStart = 3,
                    stageEnd = 5,
                    transition = LayerTransitionPreset.POP,
                    animation = PngAnimationPreset.VAIVEN,
                ),
            ),
        )

        val project = SchemeTiming.applyToProject(scheme, Project())

        assertEquals(15_000L, project.targetDurationMs)
        assertEquals(3, project.layers.size)

        val layer1 = project.layers[0]
        assertEquals(0L, layer1.startMs)
        assertEquals(6_000L, layer1.endMs)
        assertEquals(LayerTransitionPreset.POP, layer1.transition)
        assertEquals(PngAnimationPreset.FLOTAR, layer1.animation)

        val layer2 = project.layers[1]
        assertEquals(3_000L, layer2.startMs)
        assertEquals(12_000L, layer2.endMs)
        assertEquals(LayerTransitionPreset.DESLIZAR, layer2.transition)
        assertEquals(PngAnimationPreset.PULSO, layer2.animation)

        val layer3 = project.layers[2]
        assertEquals(6_000L, layer3.startMs)
        assertEquals(15_000L, layer3.endMs)

        assertEquals(1, project.backgrounds.size)
        val bg = project.backgrounds[0]
        assertEquals(0L, bg.startMs)
        assertEquals(15_000L, bg.endMs)
        assertEquals(KenBurnsMode.PANEO, bg.kenBurns)
    }

    @Test
    fun createFromProject_extractsStagesAndSanitizesPaths() {
        val initialScheme = UsageScheme(
            id = "init",
            name = "Test",
            totalDurationSec = 15,
            stagesCount = 5,
            slots = listOf(
                SchemeSlot(id = "s1", stageStart = 1, stageEnd = 2),
                SchemeSlot(id = "s2", stageStart = 2, stageEnd = 4),
            ),
        )

        val project = SchemeTiming.applyToProject(initialScheme)
        val extracted = SchemeTiming.createFromProject(
            project = project,
            id = "extracted-id",
            name = "Extracted Scheme",
            stagesCount = 5,
        )

        assertEquals("extracted-id", extracted.id)
        assertEquals("Extracted Scheme", extracted.name)
        assertEquals(15, extracted.totalDurationSec)
        assertEquals(5, extracted.stagesCount)
        assertEquals(2, extracted.slots.size)
        assertEquals(1, extracted.slots[0].stageStart)
        assertEquals(2, extracted.slots[0].stageEnd)
        assertEquals(2, extracted.slots[1].stageStart)
        assertEquals(4, extracted.slots[1].stageEnd)
    }
}
