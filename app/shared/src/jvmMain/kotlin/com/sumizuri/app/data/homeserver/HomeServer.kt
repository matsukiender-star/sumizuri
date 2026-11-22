package com.sumizuri.app.data.homeserver

import com.sumizuri.app.core.log.AppLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val Tag = "homeserver"
private const val ServerMain = "com.sumizuri.sync.server.MainKt"
private const val JarsFolder = "sync-server"
private const val PortsFile = "ports"
private const val StartWithinMs = 30_000L
private const val StopWithinSeconds = 5L

// The first ports tried, under the range systems hand out on their own.
private const val FirstLocalPort = 27_610
private const val FirstHomePort = 27_710
private const val PortsTried = 50

private val Fingerprint = Regex("fingerprint ([0-9a-f]{64})")
private val SetupCode = Regex("^ {6}([A-Z0-9]{4}(?:-[A-Z0-9]{4}){2})$")

// The server as it runs: where this computer reaches it, where phones do, and what they check. The setup code is there until someone claimed it.
class HomeServerUp(
    override val local: String,
    val homePort: Int,
    override val fingerprint: String,
    override val setupCode: String?,
) : HomeUp {
    // Where phones at home can find this computer, private addresses of real connections first.
    override fun addresses(): List<String> = homeHosts().map { "$it:$homePort" }
}

// Runs the sync server of the project on this computer. It is a process of its own, since its SQLite and the one of the app crash each other in one process.
class HomeServer(
    private val folder: File,
    private val jars: File? = bundledJars(),
    private val java: File = bundledJava(),
) : HomeHost {
    private val lock = Mutex()
    private var process: Process? = null
    private var up: HomeServerUp? = null
    private val onExit = Thread { process?.destroy() }

    val available: Boolean get() = jars?.isDirectory == true && java.isFile

    // Starts the server, or gives the one that runs. Nothing when it could not start.
    override suspend fun start(): HomeServerUp? = lock.withLock {
        up?.takeIf { process?.isAlive == true } ?: withContext(Dispatchers.IO) { launch() }
    }

    suspend fun stop() = lock.withLock {
        withContext(Dispatchers.IO) { end() }
    }

    private suspend fun launch(): HomeServerUp? {
        end()
        val classes = jars?.takeIf { it.isDirectory } ?: return null
        if (!java.isFile) return null
        folder.mkdirs()
        val (localPort, homePort) = ports() ?: return null
        val builder = ProcessBuilder(
            java.absolutePath,
            "-Xmx96m",
            "-XX:+UseSerialGC",
            "-XX:TieredStopAtLevel=1",
            "-cp",
            classes.absolutePath + File.separator + "*",
            ServerMain,
        ).directory(folder).redirectErrorStream(true)
        builder.environment().apply {
            // Settings of a server this computer may run on its own are not this server's.
            keys.removeAll { it.startsWith("SUMIZURI_") }
            put("SUMIZURI_DATA_DIR", File(folder, "data").absolutePath)
            put("SUMIZURI_HOST", "127.0.0.1")
            put("SUMIZURI_PORT", localPort.toString())
            put("SUMIZURI_TLS_PORT", homePort.toString())
            put("SUMIZURI_TRUST_PROXY", "off")
            put("SUMIZURI_SIGNUP_MODE", "closed")
            put("SUMIZURI_PARENT_PID", ProcessHandle.current().pid().toString())
        }
        val started = runCatching { builder.start() }
            .onFailure { AppLog.warn(Tag, "the home server could not start, ${it.message}") }
            .getOrNull() ?: return null
        process = started
        runCatching { Runtime.getRuntime().addShutdownHook(onExit) }

        val ready = CompletableDeferred<HomeServerUp?>()
        thread(isDaemon = true, name = "home-server") {
            var fingerprint: String? = null
            var code: String? = null
            runCatching {
                started.inputStream.bufferedReader().forEachLine { line ->
                    // What the server prints holds the setup code, so it never reaches the log of the app.
                    Fingerprint.find(line)?.let { fingerprint = it.groupValues[1] }
                    SetupCode.find(line)?.let { code = it.groupValues[1] }
                    val known = fingerprint
                    if (known != null && "Responding at" in line) {
                        ready.complete(HomeServerUp("http://127.0.0.1:$localPort", homePort, known, code))
                    }
                }
            }
            ready.complete(null)
        }
        val answer = try {
            withTimeoutOrNull(StartWithinMs) { ready.await() }
        } finally {
            // A start that was given up on never leaves a server behind.
            if (!ready.isCompleted) end()
        }
        if (answer == null) {
            end()
            AppLog.warn(Tag, "the home server did not get ready")
            return null
        }
        up = answer
        AppLog.info(Tag, "the home server is up, phones use port $homePort")
        return answer
    }

    private fun end() {
        up = null
        val running = process ?: return
        process = null
        runCatching { Runtime.getRuntime().removeShutdownHook(onExit) }
        running.destroy()
        if (!running.waitFor(StopWithinSeconds, TimeUnit.SECONDS)) running.destroyForcibly()
    }

    // The same two ports every time, since the accounts on this computer and on the phones keep them.
    private fun ports(): Pair<Int, Int>? {
        val file = File(folder, PortsFile)
        val kept = runCatching {
            file
                .readText()
                .trim()
                .split(' ')
                .map(String::toInt)
        }.getOrNull()
        if (kept?.size == 2) return kept[0] to kept[1]
        val local = freePort(FirstLocalPort) ?: return null
        val home = freePort(FirstHomePort) ?: return null
        file.writeText("$local $home")
        return local to home
    }

    private fun freePort(first: Int): Int? = (first until first + PortsTried).firstOrNull { port ->
        runCatching { ServerSocket().use { it.bind(InetSocketAddress(port)) } }.isSuccess
    }
}

private fun bundledJars(): File? {
    val packed = System.getProperty("compose.application.resources.dir")?.let(::File)
    val built = System.getProperty("sumizuri.mpv.dir")?.let { File(it).resolveSibling("common") }
    return listOfNotNull(packed, built).map { File(it, JarsFolder) }.firstOrNull { it.isDirectory }
}

private fun bundledJava(): File {
    val windows = System.getProperty("os.name").orEmpty().startsWith("Win")
    return File(System.getProperty("java.home"), if (windows) "bin/java.exe" else "bin/java")
}

// Bridges of containers and virtual machines are private too, but no phone gets in through them.
private val NotForPhones = Regex("^(docker|br-|veth|virbr|vmnet|vboxnet|podman|cni|lxc).*")

private fun homeHosts(): List<String> = runCatching {
    NetworkInterface
        .networkInterfaces()
        .toList()
        .filter { it.isUp && !it.isLoopback && !NotForPhones.matches(it.name) }
        .flatMap { it.inetAddresses().toList() }
        .filter { it.address.size == 4 }
        .mapNotNull { address: InetAddress -> address.hostAddress?.takeIf(::forPhones) }
        // The Wi-Fi or cable comes before a tunnel such as Tailscale.
        .sortedBy { if (it.startsWith("100.")) 1 else 0 }
}.getOrDefault(emptyList())

private fun forPhones(host: String): Boolean = !host.startsWith("127.") && com.sumizuri.app.data.account.SyncClient
    .isHome(host)
