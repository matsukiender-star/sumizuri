package com.sumizuri.app.source.host.net

import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.source.host.store.CookieStore
import com.sumizuri.app.source.host.store.StoredCookie
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Cookies matched in the usual way, with every change written to the cookie store and the saved ones read back at the start.
class PersistentCookiesStorage(private val scope: String, private val store: CookieStore) : CookiesStorage {
    private val matcher = AcceptAllCookiesStorage()
    private val lock = Mutex()
    private var loaded = false

    override suspend fun get(requestUrl: Url): List<Cookie> {
        ready()
        return matcher.get(requestUrl)
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        ready()
        matcher.addCookie(requestUrl, cookie)
        val domain = (cookie.domain ?: requestUrl.host).lowercase().trimStart('.')
        val path = cookie.path?.takeIf { it.startsWith("/") } ?: "/"
        val expiresAt = cookie.expires?.timestamp ?: cookie.maxAge?.let { nowMillis() + it * 1000L }
        // A cookie that is already out of date is how a site asks for it to be removed.
        if (cookie.maxAge == 0 || (expiresAt != null && expiresAt <= nowMillis())) {
            store.delete(scope, domain, path, cookie.name)
            return
        }
        store.save(
            scope,
            StoredCookie(domain, path, cookie.name, cookie.value, expiresAt, cookie.secure, cookie.httpOnly),
        )
    }

    override fun close() = matcher.close()

    // Puts the saved cookies into the matcher once, before the first request needs them.
    private suspend fun ready() {
        lock.withLock {
            if (loaded) return
            loaded = true
            for (saved in store.load(scope)) {
                val address = Url((if (saved.secure) "https://" else "http://") + saved.domain + saved.path)
                val cookie = Cookie(
                    name = saved.name,
                    value = saved.value,
                    expires = saved.expiresAt?.let { GMTDate(it) },
                    domain = saved.domain,
                    path = saved.path,
                    secure = saved.secure,
                    httpOnly = saved.httpOnly,
                )
                matcher.addCookie(address, cookie)
            }
        }
    }
}
