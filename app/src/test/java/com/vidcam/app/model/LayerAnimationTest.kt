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
}
