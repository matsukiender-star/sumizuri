package com.sumizuri.app.source.host.net

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom

// Where a redirect points. A full address is taken as it is, and a path or a query is read against the address it came from. The old query is not kept, since the new address says its own.
internal fun resolveLocation(from: String, location: String): String = when {
    location.startsWith("http://") || location.startsWith("https://") -> location

    location.startsWith("//") -> from.substringBefore("://") + ":" + location

    else -> URLBuilder(Url(from))
        .apply {
            parameters.clear()
            fragment = ""
            takeFrom(location)
        }.buildString()
}

// A name matches itself, and a name with a star in front matches the hosts under it but not the name alone.
internal fun hostMatches(pattern: String, host: String): Boolean {
    val wanted = pattern.trim().lowercase()
    return if (wanted.startsWith("*.")) host.endsWith(wanted.substring(1)) else host == wanted
}
