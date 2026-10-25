package com.sumizuri.app.data.net

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.account.DeviceSecret
import com.sumizuri.sync.core.SyncCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path

private const val SealedField = "sealed"
private const val Tag = "network"

// How a connection is doing, for the network page.
sealed interface LinkState {
    data object Idle : LinkState

    data object Connecting : LinkState

    data object Up : LinkState

    data class Failed(val message: String) : LinkState
}

// The way the app reaches the sites. Everything the app asks for goes to one router on this computer while any traffic takes a connection, and the router picks the way for each site as it is asked, so a change of the routes needs no restart. The setting belongs to this device and is sealed with its key, since it holds passwords and keys.
object Network {
    private val json = Json { ignoreUnknownKeys = true }
    private var file: Path? = null
    private var system: FileSystem = FileSystem.SYSTEM
    private val mutable = MutableStateFlow(NetworkSetting())
    val setting: StateFlow<NetworkSetting> = mutable

    // The port of the router while it runs.
    @kotlin.concurrent.Volatile
    var routerPort: Int? = null
        private set

    // The folder of the app that only this user can read, for a file a connection needs for a moment.
    val privateFolder: Path? get() = file?.parent

    // The way to a site by the setting saved now and what is known of the site.
    fun connectionFor(site: String): Connection? =
        mutable.value.connectionFor(site, SiteKinds.of(site), SiteKinds.sourcesOf(site))

    // Reads the setting once when the app starts, before anything goes to the network.
    fun load(from: Path, files: FileSystem = FileSystem.SYSTEM) {
        file = from
        system = files
        mutable.value = runCatching { read(from) }.getOrNull() ?: NetworkSetting()
        applied()
    }

    fun save(setting: NetworkSetting) {
        mutable.value = setting
        written(setting)
        applied()
    }

    // Keeps a value a connection learned, such as the fingerprint of a server, without opening it again.
    fun learned(id: String, key: String, value: String) {
        val now = mutable.value
        val next = now.copy(connections = now.connections.map { if (it.id == id) it.with(key, value) else it })
        mutable.value = next
        written(next)
    }

    private fun applied() {
        val now = mutable.value
        NetworkEngine.keep(now.inUse)
        if (now.inUse.isEmpty()) NetworkEngine.stopRouter()
        routerPort = if (now.inUse.isEmpty()) null else NetworkEngine.startRouter()
        applyRouter(routerPort)
        val ways = Traffic.entries.joinToString(
            ", ",
        ) { "${it.name.lowercase()} ${now.connectionFor(it)?.name ?: "straight"}" }
        AppLog.info(Tag, if (routerPort == null) "going straight to the sites" else "router on port $routerPort, $ways")
    }

    private fun written(setting: NetworkSetting) {
        val target = file ?: return
        runCatching {
            write(target, setting)
        }.onFailure { AppLog.warn(Tag, "the network setting could not be saved, ${it.message}") }
    }

    private fun read(from: Path): NetworkSetting? {
        if (!system.exists(from)) return null
        val text = system.read(from) { readUtf8() }
        val sealed = (json.parseToJsonElement(text) as? JsonObject)?.get(SealedField)?.jsonPrimitive?.content
        val plain = if (sealed ==
            null
        ) {
            text
        } else {
            DeviceSecret.open(SyncCrypto.fromBase64(sealed))?.decodeToString() ?: return null
        }
        return json.decodeFromString(NetworkSetting.serializer(), plain)
    }

    private fun write(target: Path, setting: NetworkSetting) {
        val plain = json.encodeToString(NetworkSetting.serializer(), setting)
        val sealed = DeviceSecret.seal(plain.encodeToByteArray())
        if (sealed ==
            null
        ) {
            AppLog.warn(Tag, "this device cannot seal secrets, the network setting is kept in a private file only")
        }
        val text = sealed?.let { buildJsonObject { put(SealedField, SyncCrypto.base64(it)) }.toString() } ?: plain
        val temporary = target.parent!! / "${target.name}.tmp"
        system.write(temporary) { writeUtf8(text) }
        DeviceSecret.keepPrivate(temporary.toString())
        system.atomicMove(temporary, target)
    }
}

// The router and the connections behind it, on the platforms that have them.
expect object NetworkEngine {
    val states: StateFlow<Map<String, LinkState>>

    // Starts the router, or keeps the one that runs, and gives its port.
    fun startRouter(): Int?

    fun stopRouter()

    // Closes the connections that are no longer in use or were changed, so they open again the new way when next needed.
    fun keep(inUse: List<Connection>)

    // Asks a page through a connection, or straight for null, which address the sites see. Gives the address and the country, or what went wrong.
    suspend fun trace(connection: Connection?): Result<Pair<String, String>>
}

// Points the parts of the platform that make connections at the router, or straight for null.
internal expect fun applyRouter(port: Int?)

// Whether this platform can send the app through other connections, so the network page shows only where it works.
expect val networkSupported: Boolean
