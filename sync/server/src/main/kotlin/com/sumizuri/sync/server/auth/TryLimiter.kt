package com.sumizuri.sync.server.auth

import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.server.ApiException
import io.ktor.http.HttpStatusCode
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

// Pauses an account or a network after too many wrong tries in a row, against guessing. It lives in memory only, and a network is known by a fingerprint, never its address.
class TryLimiter(
    private val secret: ByteArray,
    private val maxTries: () -> Int,
    private val pause: Duration = 15.minutes,
) {
    private class Tries(var count: Int = 0, var pausedUntil: Long = 0)

    private val tries = ConcurrentHashMap<String, Tries>()

    fun accountKey(usernameKey: String) = "account:$usernameKey"

    fun networkKey(address: String) = "network:" + ServerCrypto.base64(ServerCrypto.hmac(secret, address).copyOf(12))

    // Turns the request down while any of these is paused.
    fun check(vararg keys: String) {
        val now = System.currentTimeMillis()
        val until = keys.maxOfOrNull { tries[it]?.pausedUntil ?: 0 } ?: 0
        if (until > now) {
            val minutes = ((until - now) / 60_000) + 1
            throw ApiException(
                HttpStatusCode.TooManyRequests,
                ErrorCodes.TooManyTries,
                "Too many wrong tries. Try again in $minutes minutes.",
            )
        }
    }

    // Counts a wrong try, and pauses each key that reached the limit.
    fun failed(vararg keys: String) {
        val now = System.currentTimeMillis()
        for (key in keys) {
            val entry = tries.getOrPut(key) { Tries() }
            synchronized(entry) {
                entry.count++
                if (entry.count >= maxTries()) {
                    entry.pausedUntil = now + pause.inWholeMilliseconds
                    entry.count = 0
                }
            }
        }
        // Old entries are dropped now and then, so the map cannot grow without end.
        if (tries.size > 10_000) tries.entries.removeIf { it.value.pausedUntil < now && it.value.count == 0 }
    }

    fun succeeded(vararg keys: String) {
        for (key in keys) tries.remove(key)
    }
}
