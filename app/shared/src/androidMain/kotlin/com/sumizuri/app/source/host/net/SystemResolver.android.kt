package com.sumizuri.app.source.host.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

actual object SystemResolver : HostResolver {
    actual override suspend fun resolve(host: String): List<String> = withContext(Dispatchers.IO) {
        try {
            InetAddress.getAllByName(host).mapNotNull { it.hostAddress }
        } catch (_: UnknownHostException) {
            // The request itself will fail and say why.
            emptyList()
        }
    }
}
