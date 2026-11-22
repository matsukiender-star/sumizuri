package com.sumizuri.app.data.account

import com.sumizuri.sync.core.Pairing
import io.ktor.http.Url

private const val LinkStart = "sumizuri://pair?"
private const val LinkVersion = "1"
private const val MostAddresses = 6
private const val FingerprintLength = 64
private const val LongestLink = 1_000

private val HostAndPort = Regex("([A-Za-z0-9.-]{1,120}):([0-9]{1,5})")

// What the code on a computer says: where it is at home, which certificate is its own, and the secret that works once.
class PairLink(val addresses: List<String>, val fingerprint: String, val secret: ByteArray) {
    fun text(): String = buildString {
        append(LinkStart).append("v=").append(LinkVersion)
        addresses.take(MostAddresses).forEach { append("&a=").append(it) }
        append("&p=").append(fingerprint)
        append("&s=").append(secret.toHexString())
    }

    // A phone scans again when its computer changed address, so only the same certificate may take the place of an account.
    fun fits(account: SyncAccount?): Boolean = account == null || account.fingerprint == fingerprint

    companion object {
        // Nothing for a link that is not whole or that points out of the home network, since anyone can make a link.
        fun parse(text: String): PairLink? {
            val link = text.trim()
            if (link.length > LongestLink || !link.startsWith(LinkStart)) return null
            val query = runCatching { Url(link).parameters }.getOrNull() ?: return null
            if (query["v"] != LinkVersion) return null
            val addresses = query.getAll("a").orEmpty()
            if (addresses.isEmpty() || addresses.size > MostAddresses || !addresses.all(::atHome)) return null
            val fingerprint = query["p"]?.takeIf { it.length == FingerprintLength && it.isLowerHex() } ?: return null
            val secret = query["s"]
                ?.takeIf { it.length == Pairing.SecretBytes * 2 && it.isLowerHex() }
                ?.hexToByteArray()
                ?: return null
            return PairLink(addresses, fingerprint, secret)
        }

        private fun atHome(address: String): Boolean {
            val (host, port) = HostAndPort.matchEntire(address)?.destructured ?: return false
            return port.toInt() in 1..65_535 && SyncClient.isHome(host)
        }

        private fun String.isLowerHex(): Boolean = all { it in '0'..'9' || it in 'a'..'f' }
    }
}
