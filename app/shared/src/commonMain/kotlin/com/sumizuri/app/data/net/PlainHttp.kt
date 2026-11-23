package com.sumizuri.app.data.net

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.io.IOException

// Android lets plain http through for the whole app or for none of it, and Live needs it for its lists, so the app turns it down itself everywhere else. Other systems keep it as they always had it.
expect val plainHttpBlocked: Boolean

// Whether an address is plain http that this device turns down outside Live.
fun refusesPlainHttp(url: String): Boolean = plainHttpBlocked && url.trim().startsWith("http://", ignoreCase = true)

class PlainHttpRefused(url: String) : IOException("Plain http is only allowed in Live: ${url.substringBefore('?')}")

class PlainHttpConfig {
    var refuses: (Url) -> Boolean = { plainHttpBlocked && it.protocol == URLProtocol.HTTP }
}

// Turns down plain http on a client, also when a redirect leads there, since every try is sent through here.
val RefusePlainHttp = createClientPlugin("RefusePlainHttp", ::PlainHttpConfig) {
    val refuses = pluginConfig.refuses
    on(Send) { request ->
        val url = request.url.build()
        if (refuses(url)) throw PlainHttpRefused(url.toString())
        proceed(request)
    }
}
