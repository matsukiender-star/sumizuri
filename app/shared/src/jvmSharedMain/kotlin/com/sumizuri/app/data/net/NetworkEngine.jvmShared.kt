package com.sumizuri.app.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

// The site the test asks, which answers over plain HTTP with the address that asked and its country.
private const val TraceHost = "www.cloudflare.com"

actual object NetworkEngine {
    private val mutable = MutableStateFlow<Map<String, LinkState>>(emptyMap())
    actual val states: StateFlow<Map<String, LinkState>> = mutable

    private var router: Router? = null
    private val links = HashMap<String, Pair<Connection, Link>>()

    @Synchronized
    actual fun startRouter(): Int? = (router ?: Router(::linkOf, ::report).also { router = it }).port

    @Synchronized
    actual fun stopRouter() {
        router?.close()
        router = null
    }

    @Synchronized
    actual fun keep(inUse: List<Connection>) {
        val wanted = inUse.associateBy { it.id }
        val gone = links.filter { (id, pair) -> wanted[id]?.sameWayAs(pair.first) != true }.keys
        for (id in gone) links.remove(id)?.second?.close()
        mutable.update { states -> states.filterKeys { it in wanted && it !in gone } }
    }

    actual suspend fun trace(connection: Connection?): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val tube = if (connection == null) Tube.direct(TraceHost, 80) else linkOf(connection).open(TraceHost, 80)
            tube.use {
                it.output.write(
                    "GET /cdn-cgi/trace HTTP/1.0\r\nHost: $TraceHost\r\nConnection: close\r\n\r\n".encodeToByteArray(),
                )
                it.output.flush()
                val lines = it.input.readBytes().decodeToString().lines().associate { line ->
                    line.substringBefore('=') to
                        line.substringAfter('=')
                }
                (lines["ip"] ?: error("No address came back")) to (lines["loc"] ?: "?")
            }
        }.onSuccess { connection?.let { report(it.id, LinkState.Up) } }
            .onFailure { error ->
                connection?.let {
                    report(it.id, LinkState.Failed(error.message ?: error::class.simpleName.orEmpty()))
                }
            }
    }

    // The open link of a connection, made again when the connection was changed.
    @Synchronized
    private fun linkOf(connection: Connection): Link {
        links[connection.id]?.let { (was, link) ->
            if (was.sameWayAs(connection)) return link
            link.close()
        }
        val made = when (connection.type) {
            ConnectionType.Proxy -> ProxyLink(connection)
            ConnectionType.Ssh -> SshLink(connection)
            ConnectionType.WireGuard -> WireGuardLink(connection)
        }
        links[connection.id] = connection to made
        report(connection.id, LinkState.Connecting)
        return made
    }

    // Told only when it changed, since the router reports each site it opens.
    private fun report(id: String, state: LinkState) {
        if (mutable.value[id] != state) mutable.update { it + (id to state) }
    }
}
