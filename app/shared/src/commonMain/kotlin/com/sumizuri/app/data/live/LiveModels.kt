package com.sumizuri.app.data.live

import kotlinx.serialization.Serializable

// Setting key that toggles the Live tab visibility without touching sync-excluded keys.
const val ShowLiveTabKey = "live.showTab"

// Whether a playlist comes from an HTTP(S) address or a local file on the device.
@Serializable
enum class LiveSourceKind { Url, File }

// Metadata of one saved M3U playlist for the active profile.
@Serializable
data class LivePlaylist(
    val id: String,
    val name: String,
    val kind: LiveSourceKind,
    val source: String,
    val channelCount: Int,
    val updatedAt: Long,
)

// One playable live stream parsed from an M3U entry.
@Serializable
data class LiveChannel(
    val id: String,
    val playlistId: String,
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
) {
    fun headers(): Map<String, String> = buildMap {
        userAgent?.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
        referrer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
    }
}

// Outcome of adding or refreshing a playlist so the UI can show a clear message.
sealed interface LiveLoadResult {
    data class Ok(val playlist: LivePlaylist, val truncated: Boolean) : LiveLoadResult
    data object InvalidUrl : LiveLoadResult
    data object TooLarge : LiveLoadResult
    data object Empty : LiveLoadResult
    data object ReadFailed : LiveLoadResult
}

// Strips credentials and query tokens before displaying an M3U address anywhere in the UI.
fun redactLiveUrl(raw: String): String {
    val trimmed = raw.trim()
    val schemeEnd = trimmed.indexOf("://")
    if (schemeEnd < 0) return trimmed.substringAfterLast('/').substringAfterLast('\\')
    val scheme = trimmed.substring(0, schemeEnd + 3)
    val rest = trimmed.substring(schemeEnd + 3)
    val pathStart = rest.indexOf('/')
    val authority = if (pathStart >= 0) rest.substring(0, pathStart) else rest
    val pathAndQuery = if (pathStart >= 0) rest.substring(pathStart) else ""
    val hostOnly = authority.substringAfterLast('@')
    val cleanPath = pathAndQuery.substringBefore('?').substringBefore('#')
    val hasQuery = pathAndQuery.contains('?')
    return buildString {
        append(scheme)
        append(hostOnly)
        append(cleanPath)
        if (hasQuery) append("?…")
    }
}

// Checks that a stream or playlist URL uses http or https only, never file: or other schemes.
fun isHttpUrl(url: String): Boolean {
    val lower = url.trim().lowercase()
    return lower.startsWith("http://") || lower.startsWith("https://")
}
