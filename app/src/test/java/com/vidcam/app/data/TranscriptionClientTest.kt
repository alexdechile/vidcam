package com.vidcam.app.data

import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Cliente de transcripción contra un servidor HTTP mínimo en localhost.
 *
 * Se implementa con `ServerSocket` (no `com.sun.net.httpserver`, que no está en
 * el classpath de los tests unitarios de Android) y verifica el multipart que
 * se envía y la lectura de la respuesta, sin red externa.
 */
class TranscriptionClientTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var serverSocket: ServerSocket
    private var serverThread: Thread? = null
    private val responseStatus = AtomicReference(200)
    private val responseBody = AtomicReference("""{"text":"hola"}""")
    private val lastBody = AtomicReference("")
    private val lastAuthorization = AtomicReference("")
    private val lastContentType = AtomicReference("")

    @Before
    fun startServer() {
        serverSocket = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        serverThread = thread(isDaemon = true, name = "stt-test-server") {
            while (!serverSocket.isClosed) {
                val socket = runCatching { serverSocket.accept() }.getOrNull() ?: break
                runCatching { handle(socket) }
            }
        }
    }

    @After
    fun stopServer() {
        runCatching { serverSocket.close() }
        serverThread?.join(1_000)
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            val input = client.getInputStream()
            val headers = readUntilBlankLine(input)
            val length = headers.lineSequence()
                .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.toIntOrNull() ?: 0
            val body = ByteArray(length)
            var read = 0
            while (read < length) {
                val chunk = input.read(body, read, length - read)
                if (chunk < 0) break
                read += chunk
            }
            lastBody.set(String(body, Charsets.ISO_8859_1))
            lastAuthorization.set(
                headers.lineSequence()
                    .firstOrNull { it.startsWith("Authorization:", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.trim()
                    .orEmpty(),
            )
            lastContentType.set(
                headers.lineSequence()
                    .firstOrNull { it.startsWith("Content-Type:", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.trim()
                    .orEmpty(),
            )
            val payload = responseBody.get().toByteArray()
            val head = buildString {
                append("HTTP/1.1 ").append(responseStatus.get()).append(" OK\r\n")
                append("Content-Type: application/json\r\n")
                append("Content-Length: ").append(payload.size).append("\r\n")
                append("Connection: close\r\n\r\n")
            }
            client.getOutputStream().apply {
                write(head.toByteArray())
                write(payload)
                flush()
            }
        }
    }

    private fun readUntilBlankLine(input: java.io.InputStream): String {
        val builder = StringBuilder()
        while (!builder.endsWith("\r\n\r\n")) {
            val next = input.read()
            if (next < 0) break
            builder.append(next.toChar())
        }
        return builder.toString()
    }

    private fun baseUrl(path: String): String = "http://127.0.0.1:${serverSocket.localPort}$path"

    private fun sampleFile(): File {
        val file = folder.newFile("toma.wav")
        file.writeBytes(ByteArray(2_048) { (it % 120).toByte() })
        return file
    }

    @Test
    fun `envia la toma y lee los segmentos`() {
        responseStatus.set(200)
        responseBody.set(
            """{"text":"hola mundo","segments":[{"start":0.0,"end":1.2,"text":"hola"},{"start":1.2,"end":2.4,"text":"mundo"}]}""",
        )
        val client = TranscriptionClient(
            TranscriptionSettings(url = baseUrl("/transcribe"), token = "secreto", language = "es"),
        )

        val result = client.transcribe(sampleFile(), durationMs = 3_000L)

        assertTrue(result is TranscriptionResult.Success)
        val segments = (result as TranscriptionResult.Success).segments
        assertEquals(2, segments.size)
        assertEquals("hola", segments[0].text)
        assertEquals(1_200L, segments[0].endMs)
        val body = lastBody.get()
        assertTrue(body.contains("name=\"file\""))
        assertTrue(body.contains("toma.wav"))
        assertTrue(body.contains("name=\"language\""))
        assertEquals("Bearer secreto", lastAuthorization.get())
        assertTrue(lastContentType.get().startsWith("multipart/form-data"))
    }

    @Test
    fun `un error del servicio se informa con el codigo`() {
        responseStatus.set(500)
        responseBody.set("""{"detail":"modelo no cargado"}""")
        val client = TranscriptionClient(TranscriptionSettings(url = baseUrl("/transcribe")))

        val result = client.transcribe(sampleFile(), durationMs = 1_000L)

        assertTrue(result is TranscriptionResult.Failure)
        val message = (result as TranscriptionResult.Failure).message
        assertTrue(message.contains("500"))
        assertTrue(message.contains("modelo no cargado"))
    }

    @Test
    fun `sin url configurada no se intenta nada`() {
        val client = TranscriptionClient(TranscriptionSettings(url = ""))

        val result = client.transcribe(sampleFile(), durationMs = 1_000L)

        assertTrue(result is TranscriptionResult.Failure)
        assertTrue((result as TranscriptionResult.Failure).message.contains("Configura"))
    }

    @Test
    fun `una toma inexistente se informa`() {
        val client = TranscriptionClient(TranscriptionSettings(url = baseUrl("/transcribe")))

        val result = client.transcribe(File(folder.root, "no-existe.wav"), durationMs = 1_000L)

        assertTrue(result is TranscriptionResult.Failure)
    }
}
