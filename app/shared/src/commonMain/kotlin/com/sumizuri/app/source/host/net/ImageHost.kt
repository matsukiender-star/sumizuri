package com.sumizuri.app.source.host.net

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.core.log.withoutQuery
import com.sumizuri.app.source.browser.BrowserBroker
import com.sumizuri.app.source.host.int
import com.sumizuri.app.source.host.store.CookieStore
import com.sumizuri.app.source.host.store.StoredBrowserFirstFlag
import com.sumizuri.app.source.host.store.ValueStore
import com.sumizuri.app.source.host.string
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Loads a picture the way an extension would, so a site that guards its pictures with a bot check is passed by the hidden browser.
@OptIn(ExperimentalEncodingApi::class)
class ImageHost(
    private val browser: BrowserBroker,
    private val values: ValueStore,
    private val cookies: CookieStore,
    // The network engine shared with the sources, so the pictures of each site do not start a pool of their own.
    private val engine: () -> HttpClientEngine? = { null },
) {
    private val lock = Mutex()
    private val hosts = HashMap<String, HttpHost>()

    // What is known of each site, so a picture does not ask the database whether its site is guarded every time it is drawn.
    private val guarded = HashMap<String, Boolean>()

    // True once a plain request to this site met a bot check, so the next pictures go to the browser at once.
    suspend fun isGuarded(url: String): Boolean {
        val origin = originOf(url)
        lock.withLock { guarded[origin] }?.let { return it }
        val set = flagOf(origin).isSet()
        lock.withLock { guarded[origin] = set }
        return set
    }

    suspend fun markGuarded(url: String) {
        val origin = originOf(url)
        val flag = flagOf(origin)
        if (!flag.isSet()) AppLog.info(TAG, "pictures of $origin are behind a bot check, using the hidden browser")
        flag.set()
        lock.withLock { guarded[origin] = true }
    }

    // The headers are those the source asks pictures to be loaded with, such as the site it calls the referrer.
    suspend fun load(url: String, headers: Map<String, String> = emptyMap()): ByteArray {
        val host = lock.withLock { hosts.getOrPut(originOf(url)) { hostFor(originOf(url)) } }
        val answer = host.fetch(
            buildJsonObject {
                put("url", url)
                put(
                    "options",
                    buildJsonObject {
                        put("binary", true)
                        if (headers.isNotEmpty()) {
                            put(
                                "headers",
                                buildJsonObject { headers.forEach { (name, value) -> put(name, value) } },
                            )
                        }
                    },
                )
            },
        ) as JsonObject
        val status = answer.int("statusCode") ?: 0
        if (status !in 200..299) throw ImageException("answer $status for ${url.withoutQuery()}")
        val text = answer.string("bodyBase64") ?: throw ImageException("no bytes for ${url.withoutQuery()}")
        return Base64.decode(text)
    }

    private fun scopeOf(origin: String) = "$TAG:$origin"

    private fun flagOf(origin: String) = StoredBrowserFirstFlag(scopeOf(origin), values)

    private fun hostFor(origin: String) = HttpHost(
        engine = engine(),
        browser = browser,
        browserFirst = flagOf(origin),
        label = TAG,
        cookieStore = cookies,
        cookieScope = scopeOf(origin),
    )

    private fun originOf(url: String): String =
        url.substringBefore("://") + "://" + url.substringAfter("://").substringBefore('/').substringBefore('?')

    private companion object {
        const val TAG = "images"
    }
}

class ImageException(message: String) : Exception(message)
