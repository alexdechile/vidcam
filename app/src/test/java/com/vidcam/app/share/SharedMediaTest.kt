package com.vidcam.app.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Clasificación de los archivos que llegan desde otras apps. La lógica vive en
 * [SharedKinds], sin dependencias de Android, así que se prueba en la JVM sin
 * emulador ni `Intent`.
 */
class SharedMediaTest {

    @Test
    fun `reconoce el tipo por el mime declarado`() {
        assertEquals(SharedKind.VIDEO, SharedKinds.kindOf("video/mp4", null))
        assertEquals(SharedKind.IMAGE, SharedKinds.kindOf("image/jpeg", null))
        assertEquals(SharedKind.AUDIO, SharedKinds.kindOf("audio/mpeg", null))
    }

    @Test
    fun `ignora los parametros del mime`() {
        assertEquals(
            SharedKind.VIDEO,
            SharedKinds.kindOf("video/mp4; codecs=avc1", null),
        )
        assertEquals(
            SharedKind.IMAGE,
            SharedKinds.kindOf("IMAGE/PNG", null),
        )
    }

    @Test
    fun `usa la extension cuando el mime no sirve`() {
        assertEquals(SharedKind.VIDEO, SharedKinds.kindOf(null, "clip.MP4"))
        assertEquals(SharedKind.VIDEO, SharedKinds.kindOf("*/*", "clip.mov"))
        assertEquals(SharedKind.IMAGE, SharedKinds.kindOf("application/octet-stream", "foto.png"))
        assertEquals(SharedKind.AUDIO, SharedKinds.kindOf("", "pista.flac"))
    }

    @Test
    fun `marca como desconocido lo que no puede usar`() {
        assertEquals(SharedKind.UNKNOWN, SharedKinds.kindOf("application/pdf", null))
        assertEquals(SharedKind.UNKNOWN, SharedKinds.kindOf("application/pdf", "factura.pdf"))
        assertEquals(SharedKind.UNKNOWN, SharedKinds.kindOf(null, "sin_extension"))
        assertEquals(SharedKind.UNKNOWN, SharedKinds.kindOf(null, null))
    }

    @Test
    fun `el mime manda sobre la extension`() {
        assertEquals(SharedKind.IMAGE, SharedKinds.kindOf("image/png", "captura.mp4"))
    }

    @Test
    fun `extension de guardado desde el nombre`() {
        assertEquals("mp4", SharedKinds.extensionFor("video/mp4", "clip.MP4", SharedKind.VIDEO))
        assertEquals("jpeg", SharedKinds.extensionFor("image/jpeg", "foto.JPEG", SharedKind.IMAGE))
        assertEquals("mp3", SharedKinds.extensionFor("audio/mpeg", "tema.mp3", SharedKind.AUDIO))
    }

    @Test
    fun `extension de guardado desde el mime`() {
        assertEquals("mov", SharedKinds.extensionFor("video/quicktime", null, SharedKind.VIDEO))
        assertEquals("m4a", SharedKinds.extensionFor("audio/mp4", null, SharedKind.AUDIO))
        assertEquals("jpg", SharedKinds.extensionFor("image/jpeg", null, SharedKind.IMAGE))
    }

    @Test
    fun `extension de guardado por defecto segun el tipo`() {
        assertEquals("mp4", SharedKinds.extensionFor(null, "clip", SharedKind.VIDEO))
        assertEquals("png", SharedKinds.extensionFor(null, "imagen", SharedKind.IMAGE))
        assertEquals("m4a", SharedKinds.extensionFor(null, "pista", SharedKind.AUDIO))
        assertEquals("bin", SharedKinds.extensionFor(null, "otro", SharedKind.UNKNOWN))
    }

    @Test
    fun `extrae la extension del nombre visible`() {
        assertEquals("mp4", SharedKinds.extensionOf("clip.MP4"))
        assertEquals("mp4", SharedKinds.extensionOf("ruta/interna/clip.mp4"))
        assertEquals("", SharedKinds.extensionOf("sin_extension"))
        assertEquals("", SharedKinds.extensionOf(".oculto"))
        assertEquals("", SharedKinds.extensionOf(null))
    }

    @Test
    fun `normaliza mimes genericos`() {
        assertNull(SharedKinds.normalize("*/*"))
        assertNull(SharedKinds.normalize("  "))
        assertNull(SharedKinds.normalize("application/octet-stream"))
        assertEquals("video/mp4", SharedKinds.normalize(" Video/MP4 ; codecs=avc1"))
    }
}
