package com.sumizuri.app.data.net

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.network_hostkey
import com.sumizuri.app.shared.resources.network_hostkey_none
import com.sumizuri.app.shared.resources.network_key
import com.sumizuri.app.shared.resources.network_kind
import com.sumizuri.app.shared.resources.network_optional
import com.sumizuri.app.shared.resources.network_passphrase
import com.sumizuri.app.shared.resources.network_password
import com.sumizuri.app.shared.resources.network_port
import com.sumizuri.app.shared.resources.network_server
import com.sumizuri.app.shared.resources.network_ssh_user
import com.sumizuri.app.shared.resources.network_ssh_user_hint
import com.sumizuri.app.shared.resources.network_type_proxy
import com.sumizuri.app.shared.resources.network_type_ssh
import com.sumizuri.app.shared.resources.network_type_wireguard
import com.sumizuri.app.shared.resources.network_user
import com.sumizuri.app.shared.resources.network_wg_config
import com.sumizuri.app.shared.resources.network_wg_config_hint
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

// What a request is for, so each kind of title can take its own way. Other is the rest: sync, updates, trackers and pages the app cannot place.
enum class Traffic { Anime, Manga, Novel, Other }

fun trafficOf(kind: Kind): Traffic = when (kind) {
    Kind.Manga -> Traffic.Manga
    Kind.Novel -> Traffic.Novel
    Kind.Anime -> Traffic.Anime
}

// How a field of a connection is typed and kept. A file keeps its text, and the name it came with under the key and .name. A fingerprint is filled in by the app and can only be forgotten.
enum class FieldKind { Text, Number, Secret, File, Choice, Fingerprint }

// One field of a kind of connection, which the network page draws without knowing the kind.
class FieldSpec(
    val key: String,
    val label: StringResource,
    val kind: FieldKind = FieldKind.Text,
    val default: String = "",
    val required: Boolean = true,
    val hint: StringResource? = null,
    val choices: List<String> = emptyList(),
)

// The kinds of connection a person can bring, each described by its fields. A new kind is one more entry here and one more link that opens it.
enum class ConnectionType(val label: StringResource, val fields: List<FieldSpec>) {
    Proxy(
        Res.string.network_type_proxy,
        listOf(
            FieldSpec("kind", Res.string.network_kind, FieldKind.Choice, "socks5", choices = listOf("socks5", "http")),
            FieldSpec("host", Res.string.network_server),
            FieldSpec("port", Res.string.network_port, FieldKind.Number, "1080"),
            FieldSpec("user", Res.string.network_user, required = false, hint = Res.string.network_optional),
            FieldSpec(
                "password",
                Res.string.network_password,
                FieldKind.Secret,
                required = false,
                hint = Res.string.network_optional,
            ),
        ),
    ),
    Ssh(
        Res.string.network_type_ssh,
        listOf(
            FieldSpec("host", Res.string.network_server),
            FieldSpec("port", Res.string.network_port, FieldKind.Number, "22"),
            FieldSpec("user", Res.string.network_ssh_user, hint = Res.string.network_ssh_user_hint),
            FieldSpec("key", Res.string.network_key, FieldKind.File),
            FieldSpec(
                "passphrase",
                Res.string.network_passphrase,
                FieldKind.Secret,
                required = false,
                hint = Res.string.network_optional,
            ),
            FieldSpec(
                "hostKey",
                Res.string.network_hostkey,
                FieldKind.Fingerprint,
                required = false,
                hint = Res.string.network_hostkey_none,
            ),
        ),
    ),
    WireGuard(
        Res.string.network_type_wireguard,
        listOf(
            FieldSpec("config", Res.string.network_wg_config, FieldKind.File, hint = Res.string.network_wg_config_hint),
        ),
    ),
}

// A connection the person added, with the values of the fields of its kind by key.
@Serializable
data class Connection(
    val id: String,
    val name: String,
    val type: ConnectionType,
    val values: Map<String, String> = emptyMap(),
) {
    operator fun get(key: String): String = values[key] ?: type.fields
        .firstOrNull { it.key == key }
        ?.default
        .orEmpty()

    fun number(key: String): Int = get(key).toIntOrNull() ?: 0

    fun with(key: String, value: String): Connection = copy(values = values + (key to value))

    // Whether every field it needs is filled in well enough to try.
    val ready: Boolean get() = type.fields.all { fieldReady(it) }

    fun fieldReady(field: FieldSpec): Boolean {
        val value = get(field.key).trim()
        if (value.isEmpty()) return !field.required
        return when {
            field.kind == FieldKind.Number -> value.toIntOrNull() in 1..65535
            field.key == "host" -> value.none { it.isWhitespace() || it == '/' || it == '@' }
            else -> true
        }
    }

    // The server this connection reaches, which always goes straight. A WireGuard server is named in its config.
    val server: String
        get() = when (type) {
            ConnectionType.WireGuard -> wireGuardEndpoint(get("config"))?.substringBeforeLast(':').orEmpty()
            else -> get("host")
        }.trim().lowercase().trim('[', ']')

    // Two connections that would open the same way, whatever was learned about them since, such as a fingerprint.
    fun sameWayAs(other: Connection): Boolean = type == other.type && values - "hostKey" == other.values - "hostKey"
}

// The Endpoint line of a WireGuard config, as host:port.
fun wireGuardEndpoint(config: String): String? = config
    .lineSequence()
    .map { it.trim() }
    .firstOrNull { it.startsWith("Endpoint", ignoreCase = true) && '=' in it }
    ?.substringAfter('=')
    ?.trim()
