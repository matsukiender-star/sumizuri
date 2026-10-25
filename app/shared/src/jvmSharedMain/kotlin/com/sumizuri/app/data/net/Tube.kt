package com.sumizuri.app.data.net

import java.io.Closeable
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.Base64

private const val ConnectMs = 15_000

// A two way stream to a site, however it was opened: straight, through a proxy or inside a tunnel.
internal class Tube(val input: InputStream, val output: OutputStream, private val closer: () -> Unit) : Closeable {
    override fun close() {
        runCatching(closer)
    }

    companion object {
        // Straight to the site. The socket never asks the proxy chooser of the app, which would send it back to the router.
        fun direct(host: String, port: Int): Tube = socket(host, port).let { Tube(it.getInputStream(), it.getOutputStream(), it::close) }

        // Through a SOCKS5 proxy, which also looks up the name of the site.
        fun socks5(proxyHost: String, proxyPort: Int, user: String, password: String, host: String, port: Int): Tube {
            val socket = socket(proxyHost, proxyPort)
            try {
                val input = DataInputStream(socket.getInputStream())
                val output = socket.getOutputStream()
                val login = user.isNotEmpty()
                output.write(if (login) byteArrayOf(5, 2, 0, 2) else byteArrayOf(5, 1, 0))
                output.flush()
                if (input.readUnsignedByte() != 5) throw IOException("The proxy does not speak SOCKS5")
                when (input.readUnsignedByte()) {
                    0 -> Unit

                    2 -> {
                        val name = user.encodeToByteArray()
                        val secret = password.encodeToByteArray()
                        output.write(byteArrayOf(1, name.size.toByte()) + name + byteArrayOf(secret.size.toByte()) + secret)
                        output.flush()
                        input.readUnsignedByte()
                        if (input.readUnsignedByte() != 0) throw IOException("The proxy did not take the name and password")
                    }

                    else -> throw IOException("The proxy wants a way of signing in the app does not know")
                }
                val name = host.encodeToByteArray()
                output.write(byteArrayOf(5, 1, 0, 3, name.size.toByte()) + name + byteArrayOf((port shr 8).toByte(), port.toByte()))
                output.flush()
                input.readUnsignedByte()
                val code = input.readUnsignedByte()
                input.readUnsignedByte()
                val skip = when (input.readUnsignedByte()) {
                    1 -> 4
                    4 -> 16
                    3 -> input.readUnsignedByte()
                    else -> 0
                }
                input.readFully(ByteArray(skip + 2))
                if (code != 0) throw IOException("The proxy could not reach $host:$port, ${SocksErrors[code] ?: "code $code"}")
                return Tube(input, output, socket::close)
            } catch (error: Exception) {
                socket.close()
                throw error
            }
        }

        // Through an HTTP proxy, which opens a tunnel to the site with CONNECT.
        fun httpConnect(proxyHost: String, proxyPort: Int, user: String, password: String, host: String, port: Int): Tube {
            val socket = socket(proxyHost, proxyPort)
            try {
                val output = socket.getOutputStream()
                val input = socket.getInputStream()
                val login = if (user.isEmpty()) "" else "Proxy-Authorization: Basic ${Base64.getEncoder().encodeToString("$user:$password".encodeToByteArray())}\r\n"
                output.write("CONNECT $host:$port HTTP/1.1\r\nHost: $host:$port\r\n$login\r\n".encodeToByteArray())
                output.flush()
                val head = StringBuilder()
                while (!head.endsWith("\r\n\r\n")) {
                    val byte = input.read()
                    if (byte < 0 || head.length > 8192) throw IOException("The proxy closed the connection")
                    head.append(byte.toChar())
                }
                val answer = head.toString()
                if (answer.substringAfter(' ').take(3) != "200") throw IOException("The proxy answered ${answer.lineSequence().first()}")
                return Tube(input, output, socket::close)
            } catch (error: Exception) {
                socket.close()
                throw error
            }
        }

        private fun socket(host: String, port: Int): Socket = Socket(Proxy.NO_PROXY).apply {
            tcpNoDelay = true
            connect(InetSocketAddress(host, port), ConnectMs)
        }
    }
}

// Copies one side of a tube to the other, sending each piece at once, until either side ends.
internal fun pump(from: InputStream, to: OutputStream) {
    val buffer = ByteArray(32 * 1024)
    try {
        while (true) {
            val read = from.read(buffer)
            if (read < 0) break
            to.write(buffer, 0, read)
            to.flush()
        }
    } catch (_: Exception) {
    }
}

// What a SOCKS5 proxy means by each code of failure.
private val SocksErrors = mapOf(
    1 to "it failed",
    2 to "it is not allowed",
    3 to "the network cannot be reached",
    4 to "the site cannot be reached",
    5 to "the site refused the connection",
    6 to "it took too long",
)
