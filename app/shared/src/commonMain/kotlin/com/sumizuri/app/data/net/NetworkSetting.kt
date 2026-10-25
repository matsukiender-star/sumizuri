package com.sumizuri.app.data.net

import kotlinx.serialization.Serializable

// The connections the person added and which of them each kind of traffic takes. Traffic with no connection named goes straight.
@Serializable
data class NetworkSetting(
    val connections: List<Connection> = emptyList(),
    val routes: Map<Traffic, String> = emptyMap(),
    // Single sources that take another way than their kind: the id of the source and the id of a connection, or nothing for straight.
    val sourceRoutes: Map<String, String> = emptyMap(),
) {
    fun connection(id: String?): Connection? = connections.firstOrNull { it.id == id }

    // The connection a kind of traffic takes, or null for straight. A connection that is not filled in yet is passed over.
    fun connectionFor(traffic: Traffic): Connection? = connection(routes[traffic])?.takeIf { it.ready }

    // The connections some traffic takes, which are the only ones kept open.
    val inUse: List<Connection>
        get() = (
            Traffic.entries.mapNotNull(::connectionFor) +
                sourceRoutes.values.mapNotNull { id -> connection(id)?.takeIf { it.ready } }
            ).distinctBy { it.id }

    // The way to a site: the way set for a source that asked it, or else the connection of the first kind of traffic it is known for, or null for straight.
    fun connectionFor(site: String, known: Set<Traffic>, sources: Set<String> = emptySet()): Connection? {
        if (straight(site)) return null
        sources.firstNotNullOfOrNull { sourceRoutes[it] }?.let { id -> return connection(id)?.takeIf { it.ready } }
        val traffic = Traffic.entries.firstOrNull { it != Traffic.Other && it in known } ?: Traffic.Other
        return connectionFor(traffic)
    }

    // Whether a site always goes straight: the servers of the connections themselves and addresses on this computer or its home network, such as a sync server at home.
    fun straight(site: String): Boolean {
        val name = site.lowercase().trim('[', ']').trimEnd('.')
        if (inUse.any { it.server == name }) return true
        return isNearby(name)
    }
}

// Addresses that stay on this computer or its home network.
fun isNearby(host: String): Boolean {
    val name = host.lowercase().trim('[', ']')
    if (name == "localhost" || name.endsWith(".localhost") || name.endsWith(".local") ||
        name.endsWith(".lan")
    ) {
        return true
    }
    val privateSix = ':' in name && (name.startsWith("fc") || name.startsWith("fd"))
    if (name == "::1" || name.startsWith("fe80:") || privateSix) return true
    val parts = name.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4 || name.count { it == '.' } != 3) return false
    val (first, second) = parts
    return when (first) {
        127, 10 -> true
        192 -> second == 168
        172 -> second in 16..31
        169 -> second == 254
        else -> false
    }
}
