package com.sumizuri.app.data.net

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

// Sends every web address of the app to the router while it runs. It is put in place once, before any network client is made, since each client keeps the chooser it found when it was made, and after that only the port changes.
internal object ProxyRoute : ProxySelector() {
    @Volatile private var port: Int? = null
    private var system: ProxySelector? = null
    private var placed = false

    @Synchronized
    fun use(next: Int?) {
        if (!placed) {
            system = getDefault()
            setDefault(this)
            placed = true
        }
        port = next
    }

    // Plain sockets and this computer itself go straight. The router decides the rest.
    override fun select(uri: URI?): List<Proxy> {
        val now = port
        val host = uri?.host
        val web = uri?.scheme?.lowercase() in setOf("http", "https", "ws", "wss")
        if (now == null || host == null || !web || host == "localhost" || host.startsWith("127.") || host == "[::1]") {
            return system?.select(uri) ?: listOf(Proxy.NO_PROXY)
        }
        return listOf(Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", now)))
    }

    override fun connectFailed(uri: URI?, address: SocketAddress?, error: IOException?) {
        if (port == null && uri != null && address != null && error != null) system?.connectFailed(uri, address, error)
    }
}
