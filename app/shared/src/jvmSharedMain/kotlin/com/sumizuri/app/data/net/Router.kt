package com.sumizuri.app.data.net

import com.sumizuri.app.core.log.AppLog
import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

// A SOCKS5 proxy on this computer that everything in the app goes through while some traffic takes a connection. For each site it asks the setting saved now which way to take, so a change of the routes counts at once. It listens on the loopback address only, so nothing else on the network can use it.
internal class Router(private val linkOf: (Connection) -> Link, private val report: (String, LinkState) -> Unit) {
    private val server = ServerSocket(0, 128, InetAddress.getLoopbackAddress())
    val port: Int = server.localPort
    private val workers = Executors.newCachedThreadPool { task ->
        Thread(
            task,
            "network router",
        ).apply { isDaemon = true }
    }

    init {
        thread(name = "network router listen", isDaemon = true) {
            while (!server.isClosed) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                workers.execute { serve(client) }
            }
        }
    }

    fun close() {
        runCatching { server.close() }
        workers.shutdownNow()
    }

    private fun serve(client: Socket) {
        client.tcpNoDelay = true
        val input = DataInputStream(client.getInputStream())
        val output = client.getOutputStream()
        val (host, port) = runCatching { handshake(input, output) }.getOrNull() ?: return client.close()
        val connection = Network.connectionFor(host)
        val tube = try {
            if (connection ==
                null
            ) {
                Tube.direct(host, port)
            } else {
                linkOf(connection).open(host, port).also { report(connection.id, LinkState.Up) }
            }
        } catch (error: Exception) {
            val message = error.message ?: error::class.simpleName.orEmpty()
            connection?.let { report(it.id, LinkState.Failed(message)) }
            AppLog.warn(
                "network",
                "could not reach $host:$port ${connection?.let { "through ${it.name}" } ?: "straight"}, $message",
            )
            reply(output, 5)
            return client.close()
        }
        reply(output, 0)
        workers.execute {
            pump(input, tube.output)
            tube.close()
            runCatching { client.close() }
        }
        pump(tube.input, output)
        tube.close()
        runCatching { client.close() }
    }

    // Reads the greeting and the request of a client, and gives the site it wants. Only a plain connect without a login is taken, since only this computer can reach the router.
    private fun handshake(input: DataInputStream, output: OutputStream): Pair<String, Int>? {
        if (input.readUnsignedByte() != 5) return null
        val methods = ByteArray(input.readUnsignedByte()).also(input::readFully)
        if (0.toByte() !in methods) {
            output.write(byteArrayOf(5, 0xFF.toByte()))
            return null
        }
        output.write(byteArrayOf(5, 0))
        output.flush()
        if (input.readUnsignedByte() != 5) return null
        val command = input.readUnsignedByte()
        input.readUnsignedByte()
        val host = when (input.readUnsignedByte()) {
            1 -> InetAddress.getByAddress(ByteArray(4).also(input::readFully)).hostAddress

            3 -> ByteArray(input.readUnsignedByte()).also(input::readFully).decodeToString()

            4 -> InetAddress.getByAddress(ByteArray(16).also(input::readFully)).hostAddress

            else -> {
                reply(output, 8)
                return null
            }
        }
        val port = input.readUnsignedShort()
        if (command != 1) {
            reply(output, 7)
            return null
        }
        return host to port
    }

    private fun reply(output: OutputStream, code: Int) {
        runCatching {
            output.write(byteArrayOf(5, code.toByte(), 0, 1, 0, 0, 0, 0, 0, 0))
            output.flush()
        }
    }
}
