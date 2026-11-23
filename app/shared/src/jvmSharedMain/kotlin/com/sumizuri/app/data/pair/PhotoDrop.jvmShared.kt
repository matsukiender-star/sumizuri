package com.sumizuri.app.data.pair

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom

// The most a phone may send. The page shrinks the photo to a small square first, so this is far above it.
private const val MaxPhoto = 4 * 1024 * 1024

// How long a phone may take to send once it has connected.
private const val ReadMs = 15_000

// Listens like the pairing does, under a path no one can guess, so only the phone that read the code reaches it.
actual fun openPhotoDrop(page: String): PhotoDrop? = runCatching {
    val here = homeAddress() ?: return null
    val server = ServerSocket()
    server.bind(InetSocketAddress(0))
    val token = token()
    SocketPhotoDrop(server, token, page, "http://$here:${server.localPort}/$token")
}.getOrNull()

private class SocketPhotoDrop(
    private val server: ServerSocket,
    private val token: String,
    private val page: String,
    override val url: String,
) : PhotoDrop {
    override suspend fun receive(): ByteArray = withContext(Dispatchers.IO) {
        var body: ByteArray? = null
        while (body == null) body = server.accept().use { serve(it) }
        body
    }

    override fun close() {
        runCatching { server.close() }
    }

    // Answers one request: the page for a visit to the path, and the photo for a post to it. Gives the photo, or null for anything else.
    private fun serve(socket: Socket): ByteArray? {
        socket.soTimeout = ReadMs
        val input = BufferedInputStream(socket.getInputStream())
        val parts = line(input)?.split(' ') ?: return null
        var length = 0
        while (true) {
            val header = line(input) ?: return null
            if (header.isEmpty()) break
            if (header.substringBefore(':').trim().equals("Content-Length", ignoreCase = true)) {
                length = header.substringAfter(':').trim().toIntOrNull() ?: 0
            }
        }
        val ours = parts.size >= 2 && parts[1].substringBefore('?').trimStart('/') == token
        val out = socket.getOutputStream()
        return when {
            ours && parts[0] == "GET" -> null.also { answer(out, "200 OK", page) }

            ours && parts[0] == "POST" && length in 1..MaxPhoto -> bodyOf(input, length).also {
                answer(out, if (it != null) "200 OK" else "400 Bad Request", "")
            }

            else -> null.also { answer(out, "404 Not Found", "") }
        }
    }
}

private fun answer(out: OutputStream, status: String, html: String) {
    val body = html.encodeToByteArray()
    out.write(
        (
            "HTTP/1.1 $status\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\n" +
                "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
            ).encodeToByteArray(),
    )
    out.write(body)
    out.flush()
}

// Twelve letters and digits that are not easy to mix up.
private fun token(): String {
    val letters = "abcdefghjkmnpqrstuvwxyz23456789"
    val random = SecureRandom()
    return buildString { repeat(12) { append(letters[random.nextInt(letters.length)]) } }
}
