package com.sumizuri.app.data.net

import com.sumizuri.app.core.log.AppLog
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

private const val Tag = "wireguard"
private const val StartMs = 15_000L

// The wireproxy program that comes with the app, or null where this build has none.
internal expect fun wireproxyBinary(): File?

// Ties a helper the app started to the life of the app, so it ends with the app even when the app is stopped from outside.
internal expect fun tieToApp(process: Process)

// A WireGuard config the person brings, run by wireproxy, which keeps the whole tunnel inside the app and offers it as a SOCKS5 proxy on this computer. It needs no rights of an administrator and no VPN of the system, so another VPN can run beside it.
internal class WireGuardLink(private val connection: Connection) : Link {
    private var process: Process? = null
    private var port = 0

    // A tunnel whose server never answers looks to wireproxy like a site it cannot reach, so that is said plainly.
    override fun open(host: String, port: Int): Tube = try {
        Tube.socks5("127.0.0.1", started(), "", "", host, port)
    } catch (error: IOException) {
        if (error.message?.endsWith("cannot be reached") != true) throw error
        throw IOException(
            "The WireGuard server did not answer. Check the Endpoint of the config and that its UDP port is open.",
        )
    }

    @Synchronized
    override fun close() {
        process?.destroy()
        process = null
    }

    // Starts wireproxy when it does not run, and gives the port of its SOCKS5 proxy.
    @Synchronized
    private fun started(): Int {
        process?.takeIf { it.isAlive }?.let { return port }
        val program = wireproxyBinary() ?: throw IOException("This build of the app has no WireGuard")
        val free = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }
        // The config holds the private key, so it is written only for the start, in the folder of the app that only this user can read, and deleted once wireproxy has read it.
        val folder = Network.privateFolder
            ?.toString()
            ?.let(::File)
            ?.takeIf { it.isDirectory }
        val file = File.createTempFile("wg-start", ".conf", folder)
        val said = ArrayDeque<String>()
        try {
            file.writeText(configFor(connection["config"], free))
            file.setReadable(false, false)
            file.setReadable(true, true)
            val made = ProcessBuilder(program.absolutePath, "-c", file.absolutePath).redirectErrorStream(true).start()
            tieToApp(made)
            thread(name = "wireproxy log", isDaemon = true) {
                made.inputStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                    synchronized(said) {
                        said.addLast(line)
                        if (said.size > 8) said.removeFirst()
                    }
                    AppLog.debug(Tag, line)
                }
            }
            val until = System.currentTimeMillis() + StartMs
            while (!listening(free)) {
                if (!made.isAlive || System.currentTimeMillis() > until) {
                    made.destroy()
                    val last = synchronized(said) { said.lastOrNull() }
                    throw IOException("WireGuard did not start" + (last?.let { ": $it" } ?: ""))
                }
                Thread.sleep(100)
            }
            AppLog.info(Tag, "${connection.name} runs, its proxy is on port $free")
            process = made
            port = free
            return free
        } finally {
            file.delete()
        }
    }

    private fun listening(port: Int): Boolean = runCatching {
        Socket(
            java.net.Proxy.NO_PROXY,
        ).use { it.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 200) }
    }.isSuccess
}

// The config of the person with the SOCKS5 proxy wireproxy should offer. The name of the server is looked up here, since wireproxy cannot find the name servers of a phone, and a name server is added when the config has none, so the names of sites are looked up inside the tunnel.
internal fun configFor(config: String, port: Int): String {
    val lines = config
        .lines()
        .map { line ->
            val key = line.substringBefore('=').trim()
            if (!key.equals("Endpoint", ignoreCase = true)) return@map line
            val endpoint = line.substringAfter('=').trim()
            val host = endpoint.substringBeforeLast(':').trim('[', ']')
            val address = runCatching { InetAddress.getByName(host).hostAddress }.getOrNull() ?: return@map line
            val shown = if (':' in address) "[$address]" else address
            "Endpoint = $shown:${endpoint.substringAfterLast(':')}"
        }.filterNot { it.trim().startsWith("[Socks5]", ignoreCase = true) }
    val hasDns = lines.any { it.substringBefore('=').trim().equals("DNS", ignoreCase = true) }
    val withDns = if (hasDns) {
        lines
    } else {
        lines.flatMap {
            if (it.trim().equals(
                    "[Interface]",
                    ignoreCase = true,
                )
            ) {
                listOf(it, "DNS = 1.1.1.1")
            } else {
                listOf(it)
            }
        }
    }
    return withDns.joinToString("\n") + "\n\n[Socks5]\nBindAddress = 127.0.0.1:$port\n"
}
