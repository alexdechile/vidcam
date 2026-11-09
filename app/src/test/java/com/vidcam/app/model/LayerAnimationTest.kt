package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerAnimationTest {

    private fun layer(
        keyframes: List<LayerKeyframe> = emptyList(),
        x: Float = 0.5f,
        y: Float = 0.5f,
        scale: Float = 1f,
        rotationDeg: Float = 0f,
        endMs: Long = 3_000L,
    ) = OverlayLayer(
        id = "layer-1",
        kind = LayerKind.PNG,
        x = x,
        y = y,
        scale = scale,
        rotationDeg = rotationDeg,
        endMs = endMs,
        keyframes = keyframes,
    )

    @Test
    fun layerWithoutKeyframesKeepsStaticTransform() {
        val result = layer(x = 0.25f, scale = 2f).resolvedAt(1_500L)
        assertEquals(0.25f, result.x, 0.0001f)
        assertEquals(2f, result.scale, 0.0001f)
        assertTrue(result.keyframes.isEmpty())
    }

    @Test
    fun interpolatesBetweenKeyframes() {
        val animated = layer(
            keyframes = listOf(
                LayerKeyframe(0L, 0.2f, 0.8f, 1f, 0f),
                LayerKeyframe(2_000L, 0.6f, 0.4f, 3f, 90f),
            ),
        )
        val middle = animated.resolvedAt(1_000L)
        assertEquals(0.4f, middle.x, 0.0001f)
        assertEquals(0.6f, middle.y, 0.0001f)
        assertEquals(2f, middle.scale, 0.0001f)
        assertEquals(45f, middle.rotationDeg, 0.0001f)
    }

    @Test
    fun holdsEndValuesOutsideKeyframeRange() {
        val animated = layer(
            keyframes = listOf(
                LayerKeyframe(500L, 0.1f, 0.1f, 1f, 0f),
                LayerKeyframe(1_500L, 0.9f, 0.9f, 2f, 0f),
            ),
        )
        assertEquals(0.1f, animated.resolvedAt(0L).x, 0.0001f)
        assertEquals(0.9f, animated.resolvedAt(3_000L).x, 0.0001f)
    }

    @Test
    fun interpolatesRotationByShortestPath() {
        val animated = layer(
            keyframes = listOf(
                LayerKeyframe(0L, 0.5f, 0.5f, 1f, 350f),
                LayerKeyframe(1_000L, 0.5f, 0.5f, 1f, 10f),
            ),
        )
        assertEquals(360f, animated.resolvedAt(500L).rotationDeg, 0.01f)
    }

    @Test
    fun upsertKeyframeKeepsTimeOrderAndReplacesSameTime() {
        val animated = layer(keyframes = listOf(LayerKeyframe(1_000L, 0.5f, 0.5f, 1f, 0f)))
        val inserted = animated
            .upsertKeyframe(LayerKeyframe(500L, 0.4f, 0.4f, 1f, 0f))
            .upsertKeyframe(LayerKeyframe(1_000L, 0.7f, 0.7f, 1f, 0f))
        assertEquals(listOf(500L, 1_000L), inserted.keyframes.map { it.timeMs })
        assertEquals(0.7f, inserted.keyframes[1].x, 0.0001f)
        assertEquals(2, inserted.keyframes.size)
    }

    @Test
    fun removeKeyframeDropsOnlyThatTime() {
        val animated = layer(
            keyframes = listOf(
                LayerKeyframe(0L, 0.1f, 0.1f, 1f, 0f),
                LayerKeyframe(1_000L, 0.5f, 0.5f, 1f, 0f),
            ),
        )
        val remaining = animated.removeKeyframeAt(1_000L)
        assertEquals(listOf(0L), remaining.keyframes.map { it.timeMs })
    }

    @Test
    fun seedKeyframesOnlyWhenThereIsNoAnimation() {
        val seeded = layer(x = 0.3f).seedKeyframesAt(0L)
        assertEquals(1, seeded.keyframes.size)
        assertEquals(0.3f, seeded.keyframes.first().x, 0.0001f)

        val alreadyAnimated = layer(
            keyframes = listOf(LayerKeyframe(0L, 0.1f, 0.1f, 1f, 0f)),
        )
        assertEquals(1, alreadyAnimated.seedKeyframesAt(0L).keyframes.size)
    }

    @Test
    fun clearAnimationHoldsLastTransform() {
        val animated = layer(
            keyframes = listOf(
                LayerKeyframe(0L, 0.1f, 0.1f, 1f, 0f),
                LayerKeyframe(3_000L, 0.9f, 0.9f, 2f, 0f),
            ),
        )
        val cleared = animated.clearAnimation()
        assertTrue(cleared.keyframes.isEmpty())
        assertEquals(0.9f, cleared.x, 0.0001f)
        assertEquals(2f, cleared.scale, 0.0001f)
    }

    @Test
    fun samplingAlwaysKeepsFirstAndSameTime() {
        val candidate = LayerKeyframe(100L, 0.5f, 0.5f, 1f, 0f)
        assertTrue(shouldSampleKeyframe(null, candidate))
        assertTrue(shouldSampleKeyframe(candidate, candidate.copy(x = 0.5001f)))
    }

    @Test
    fun samplingDecimatesSlowSmallMovesButKeepsFastOnes() {
        val previous = LayerKeyframe(1_000L, 0.5f, 0.5f, 1f, 0f)
        assertFalse(
            shouldSampleKeyframe(previous, LayerKeyframe(1_010L, 0.5005f, 0.5f, 1f, 0f)),
        )
        assertTrue(
            shouldSampleKeyframe(previous, LayerKeyframe(1_010L, 0.7f, 0.5f, 1f, 0f)),
        )
        assertTrue(
            shouldSampleKeyframe(previous, LayerKeyframe(1_100L, 0.5f, 0.5f, 1f, 0f)),
        )
    }

    @Test
    fun hitTestFindsLayerUnderPoint() {
        val target = LayerHitTarget(layer(), widthPx = 20f, heightPx = 10f)
        assertEquals(
            "layer-1",
            hitTestLayers(listOf(target), 50f, 50f, 100f, 100f)?.id,
        )
        assertNull(hitTestLayers(listOf(target), 50f, 70f, 100f, 100f))
    }

    @Test
    fun hitTestPrefersTopmostLayer() {
        val bottom = LayerHitTarget(layer(), widthPx = 40f, heightPx = 40f)
        val top = LayerHitTarget(
            layer().copy(id = "layer-2"),
            widthPx = 40f,
            heightPx = 40f,
        )
        assertEquals(
            "layer-2",
            hitTestLayers(listOf(bottom, top), 50f, 50f, 100f, 100f)?.id,
        )
    }

    @Test
    fun hitTestAccountsForScaleAndRotation() {
        val scaled = LayerHitTarget(layer(scale = 2f), widthPx = 20f, heightPx = 10f)
        assertEquals(
            "layer-1",
            hitTestLayers(listOf(scaled), 65f, 50f, 100f, 100f)?.id,
        )

        val rotated = LayerHitTarget(layer(rotationDeg = 90f), widthPx = 20f, heightPx = 10f)
        assertEquals(
            "layer-1",
            hitTestLayers(listOf(rotated), 50f, 58f, 100f, 100f)?.id,
        )
        assertNull(hitTestLayers(listOf(rotated), 58f, 50f, 100f, 100f))
    }

    @Test
    fun kenBurnsZoomInProgressesFasterWithHigherSpeed() {
        val normal = layer(endMs = 10_000L).copy(
            animation = PngAnimationPreset.KEN_BURNS_ACERCAR,
            animationSpeed = AnimationSpeed.NORMAL,
        )
        val rapido = normal.copy(animationSpeed = AnimationSpeed.RAPIDO)
        val turbo = normal.copy(animationSpeed = AnimationSpeed.TURBO)

        val t = 400L
        val scaleNormal = normal.resolvedAt(t).scale
        val scaleRapido = rapido.resolvedAt(t).scale
        val scaleTurbo = turbo.resolvedAt(t).scale

        assertTrue(scaleNormal > 1.0f)
        assertTrue(scaleRapido > scaleNormal)
        assertTrue(scaleTurbo > scaleRapido)
    }

    @Test
    fun kenBurnsZoomOutStartsZoomedInAndShrinks() {
        val zoomOut = layer(endMs = 3_000L).copy(
            animation = PngAnimationPreset.KEN_BURNS_ALEJAR,
            animationSpeed = AnimationSpeed.RAPIDO,
        )
        val atStart = zoomOut.resolvedAt(0L)
        val atPeak = zoomOut.resolvedAt(1_500L)
        assertTrue(atStart.scale > 1.4f)
        assertEquals(1.0f, atPeak.scale, 0.01f)
    }

    @Test
    fun loopPresetsProduceDynamicTransformations() {
        val pulso = layer().copy(animation = PngAnimationPreset.PULSO)
        val flotar = layer().copy(animation = PngAnimationPreset.FLOTAR)
        val vaiven = layer().copy(animation = PngAnimationPreset.VAIVEN)
        val temblor = layer().copy(animation = PngAnimationPreset.TEMBLOR)

        assertTrue(pulso.resolvedAt(200L).scale > 1.0f)
        assertTrue(kotlin.math.abs(flotar.resolvedAt(150L).y - 0.5f) > 0.001f)
        assertTrue(kotlin.math.abs(vaiven.resolvedAt(70L).rotationDeg) > 0.5f)
        assertTrue(temblor.resolvedAt(250L).scale >= 1.04f)
    }

    @Test
    fun transitionsAffectEntranceMiddleAndExit() {
        val popLayer = layer(endMs = 2_000L).copy(transition = LayerTransitionPreset.POP)
        assertTrue(popLayer.resolvedAt(0L).scale < 0.25f)
        assertEquals(1.0f, popLayer.resolvedAt(1_000L).scale, 0.001f)
        assertTrue(popLayer.resolvedAt(2_000L).scale < 0.25f)

        val slideLayer = layer(endMs = 2_000L).copy(transition = LayerTransitionPreset.DESLIZAR)
        assertTrue(slideLayer.resolvedAt(0L).y > 0.65f)
        assertEquals(0.5f, slideLayer.resolvedAt(1_000L).y, 0.001f)

        val fadeLayer = layer(endMs = 2_000L).copy(transition = LayerTransitionPreset.FUNDIDO)
        assertEquals(0f, fadeLayer.resolvedAlphaAt(0L), 0.001f)
        assertEquals(1f, fadeLayer.resolvedAlphaAt(1_000L), 0.001f)
        assertEquals(0f, fadeLayer.resolvedAlphaAt(2_000L), 0.001f)
    }

    @Test
    fun backgroundPresetsAlwaysMaintainCoverScaleMargin() {
        val sampleTimes = listOf(0L, 120L, 450L, 900L, 1_500L, 2_800L)
        for (mode in KenBurnsMode.entries) {
            for (speed in AnimationSpeed.entries) {
                val bg = BackgroundLayer(
                    id = "bg-test",
                    uri = "file:///bg.png",
                    startMs = 0L,
                    endMs = 5_000L,
                    kenBurns = mode,
                    kenBurnsSpeed = speed,
                )
                for (t in sampleTimes) {
                    val tr = bg.resolvedTransformAt(t)
                    val maxShift = maxOf(kotlin.math.abs(tr.dx), kotlin.math.abs(tr.dy))
                    assertTrue(
                        "Fondo con $mode ($speed) en t=$t debe cubrir desplazamiento",
                        tr.scale - 1f >= maxShift * 1.99f,
                    )
                }
            }
        }
    }
}
