package com.vidcam.app.model.scheme

import com.vidcam.app.model.AnimationSpeed
import com.vidcam.app.model.CaptionStyle
import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerTransitionPreset
import com.vidcam.app.model.PngAnimationPreset
import com.vidcam.app.model.VideoWindow
import com.vidcam.app.model.VideoWindowAspect
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageSchemeSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun roundTripSerialization_preservesAllFields() {
        val scheme = UsageScheme(
            id = "cascade-5-stages-15s",
            name = "5 Etapas en Cascada",
            description = "Fondo 15s, slots secuenciales solapados y música",
            totalDurationSec = 15,
            stagesCount = 5,
            background = SchemeBackground(
                kenBurns = KenBurnsMode.PANEO,
                kenBurnsSpeed = AnimationSpeed.RAPIDO,
                stageStart = 1,
                stageEnd = 5,
                label = "Fondo Paneo",
                placeholder = "gradient_sunset",
            ),
            videoWindow = VideoWindow(
                aspect = VideoWindowAspect.CUADRADO,
                x = 0.5f,
                y = 0.4f,
                scale = 0.85f,
                rotationDeg = 0f,
            ),
            slots = listOf(
                SchemeSlot(
                    id = "img1",
                    kind = SlotKind.IMAGE,
                    label = "Intro",
                    stageStart = 1,
                    stageEnd = 2,
                    transitionIn = LayerTransitionPreset.POP,
                    transitionOut = LayerTransitionPreset.FUNDIDO,
                    animation = PngAnimationPreset.FLOTAR,
                    animationSpeed = AnimationSpeed.RAPIDO,
                ),
                SchemeSlot(
                    id = "txt1",
                    kind = SlotKind.TEXT,
                    label = "Titular",
                    stageStart = 1,
                    stageEnd = 3,
                    text = "GRAN NOTICIA",
                    fontName = "Bangers",
                    fontSizeSp = 48f,
                    transitionIn = LayerTransitionPreset.DESLIZAR,
                ),
            ),
            audio = SchemeAudio(
                stageStart = 1,
                stageEnd = 5,
                fadeInSec = 0.5f,
                fadeOutSec = 1.0f,
                musicVolume = 0.9f,
                duckingAmount = 0.6f,
            ),
            captionStyle = CaptionStyle(
                fontSizeSp = 28f,
                positionY = 0.85f,
                allCaps = true,
                hasBackground = true,
            ),
        )

        val serialized = json.encodeToString(UsageScheme.serializer(), scheme)
        assertTrue(serialized.contains("\"totalDurationSec\": 15"))
        assertTrue(serialized.contains("\"stagesCount\": 5"))
        assertTrue(serialized.contains("\"PANEO\""))

        val deserialized = json.decodeFromString(UsageScheme.serializer(), serialized)
        assertEquals(scheme.id, deserialized.id)
        assertEquals(scheme.name, deserialized.name)
        assertEquals(scheme.totalDurationSec, deserialized.totalDurationSec)
        assertEquals(scheme.stagesCount, deserialized.stagesCount)
        assertEquals(scheme.background?.kenBurns, deserialized.background?.kenBurns)
        assertEquals(scheme.videoWindow.aspect, deserialized.videoWindow.aspect)
        assertEquals(2, deserialized.slots.size)
        assertEquals("GRAN NOTICIA", deserialized.slots[1].text)
        assertEquals(LayerTransitionPreset.POP, deserialized.slots[0].transitionIn)
        assertEquals(0.6f, deserialized.audio?.duckingAmount ?: 0f, 0.001f)
    }

    @Test
    fun deserialization_ignoresUnknownKeys() {
        val jsonString = """
            {
              "id": "future-scheme",
              "name": "Esquema Futuro",
              "totalDurationSec": 20,
              "stagesCount": 4,
              "newFutureField": "ignored_value",
              "anotherNewFeature": 12345
            }
        """.trimIndent()

        val decoded = json.decodeFromString(UsageScheme.serializer(), jsonString)
        assertEquals("future-scheme", decoded.id)
        assertEquals("Esquema Futuro", decoded.name)
        assertEquals(20, decoded.totalDurationSec)
        assertEquals(4, decoded.stagesCount)
    }
}
