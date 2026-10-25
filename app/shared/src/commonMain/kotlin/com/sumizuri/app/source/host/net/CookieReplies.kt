package com.sumizuri.app.source.host.net

import com.sumizuri.app.source.host.BridgeException
import com.sumizuri.app.source.host.bool
import com.sumizuri.app.source.host.int
import com.sumizuri.app.source.host.string
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// The cookies a source holds for an address, as the reply to its cookies.get call.
internal suspend fun CookiesStorage.repliesFor(arguments: JsonObject): JsonElement {
    val url = arguments.string("url") ?: throw BridgeException("cookies.get needs a url")
    val found = get(Url(url))
    return buildJsonObject {
        put(
            "cookies",
            buildJsonArray {
                found.forEach { cookie ->
                    add(
                        buildJsonObject {
                            put("name", cookie.name)
                            put("value", cookie.value)
                        },
                    )
                }
            },
        )
    }
}

// Keeps the cookie a source gave with its cookies.set call.
internal suspend fun CookiesStorage.keep(arguments: JsonObject): JsonElement {
    val url = arguments.string("url") ?: throw BridgeException("cookies.set needs a url")
    val cookie = Cookie(
        name = arguments.string("name") ?: throw BridgeException("cookies.set needs a name"),
        value = arguments.string("value") ?: "",
        domain = arguments.string("domain"),
        path = arguments.string("path"),
        maxAge = arguments.int("maxAge"),
        secure = arguments.bool("secure") ?: false,
        httpOnly = arguments.bool("httpOnly") ?: false,
    )
    addCookie(Url(url), cookie)
    return buildJsonObject { put("ok", true) }
}
