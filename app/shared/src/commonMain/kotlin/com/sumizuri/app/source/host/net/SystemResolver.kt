package com.sumizuri.app.source.host.net

// The lookup the platform itself provides.
expect object SystemResolver : HostResolver {
    override suspend fun resolve(host: String): List<String>
}

// Looks up the addresses a host name points at, so a public looking name cannot hide a private address.
fun interface HostResolver {
    suspend fun resolve(host: String): List<String>
}
