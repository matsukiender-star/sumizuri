package com.sumizuri.app.data.live

// Hard caps for untrusted M3U files so huge playlists cannot exhaust memory.
const val MaxLivePlaylistBytes = 20L * 1024L * 1024L
const val MaxLiveChannels = 50_000
private const val MaxExtInfHeaderChars = 8 * 1024

// Result of parsing M3U text, noting whether the channel cap stopped reading early.
data class M3uParseResult(val channels: List<LiveChannel>, val truncated: Boolean)

// Parses M3U / M3U8 playlists line by line, keeping only http/https streams and safe headers.
object M3uParser {
    // Negative lookbehind prevents quadratic backtracking on long attribute-free #EXTINF lines.
    private val attrRegex = Regex("""(?<![a-zA-Z0-9_-])([a-zA-Z0-9_-]+)=("([^"]*)"|'([^']*)'|([^\s,]+))""")

    fun parse(playlistId: String, text: String): M3uParseResult {
        val channels = ArrayList<LiveChannel>()
        val seenIds = HashSet<String>()
        var pendingName = ""
        var pendingGroup = ""
        var pendingLogo: String? = null
        var pendingUserAgent: String? = null
        var pendingReferrer: String? = null
        var truncated = false

        var pos = 0
        val length = text.length
        while (pos < length) {
            val end = text.indexOf('\n', pos).let { if (it < 0) length else it }
            val line = text.substring(pos, end).trim().removePrefix("\uFEFF")
            pos = end + 1
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val split = splitExtInf(line.substring(8))
                    val header =
                        if (split.first.length > MaxExtInfHeaderChars) {
                            split.first.substring(0, MaxExtInfHeaderChars)
                        } else {
                            split.first
                        }
                    val attrs = parseAttributes(header)
                    pendingName = split.second.trim()
                    pendingGroup = attrs["group-title"]?.trim().orEmpty()
                    pendingLogo = attrs["tvg-logo"]?.trim()?.takeIf(::isHttpUrl)
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val group = line.substring(8).trim()
                    if (group.isNotEmpty() && pendingGroup.isEmpty()) pendingGroup = group
                }

                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val option = line.substring(11).trim()
                    when {
                        option.startsWith("http-user-agent=", ignoreCase = true) -> {
                            pendingUserAgent = option.substringAfter('=').trim().takeIf { it.isNotEmpty() }
                        }

                        option.startsWith("http-referrer=", ignoreCase = true) -> {
                            pendingReferrer = option.substringAfter('=').trim().takeIf { it.isNotEmpty() }
                        }

                        option.startsWith("http-referer=", ignoreCase = true) -> {
                            pendingReferrer = option.substringAfter('=').trim().takeIf { it.isNotEmpty() }
                        }
                    }
                }

                line.startsWith("#") -> Unit

                else -> {
                    if (isHttpUrl(line)) {
                        val displayName = pendingName.ifBlank { fallbackName(line) }
                        val id = stableChannelId(playlistId, line, displayName)
                        if (seenIds.add(id)) {
                            if (channels.size >= MaxLiveChannels) {
                                truncated = true
                                break
                            }
                            channels.add(
                                LiveChannel(
                                    id = id,
                                    playlistId = playlistId,
                                    name = displayName,
                                    url = line,
                                    group = pendingGroup,
                                    logo = pendingLogo,
                                    userAgent = pendingUserAgent,
                                    referrer = pendingReferrer,
                                ),
                            )
                        }
                    }
                    // Reset entry metadata after every URL line, even skipped schemes.
                    pendingName = ""
                    pendingGroup = ""
                    pendingLogo = null
                    pendingUserAgent = null
                    pendingReferrer = null
                }
            }
        }
        return M3uParseResult(channels, truncated)
    }

    // Splits the #EXTINF header from the channel title on the first comma outside quotes.
    private fun splitExtInf(body: String): Pair<String, String> {
        var inDouble = false
        var inSingle = false
        for (i in body.indices) {
            when (body[i]) {
                '"' -> if (!inSingle) inDouble = !inDouble
                '\'' -> if (!inDouble) inSingle = !inSingle
                ',' -> if (!inDouble && !inSingle) return body.substring(0, i) to body.substring(i + 1)
            }
        }
        return body to ""
    }

    private fun parseAttributes(header: String): Map<String, String> {
        val result = HashMap<String, String>()
        for (match in attrRegex.findAll(header)) {
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[3].ifEmpty { match.groupValues[4].ifEmpty { match.groupValues[5] } }
            result[key] = value
        }
        return result
    }

    private fun fallbackName(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#').trimEnd('/')
        return clean.substringAfterLast('/').ifBlank { "Channel" }
    }

    // FNV-1a 64-bit hash so channel IDs stay stable across playlist refreshes without exposing credentials.
    private fun stableChannelId(playlistId: String, url: String, name: String): String {
        var hash = -3750763034362895579L
        val input = "$playlistId|$url|$name"
        for (ch in input) {
            hash = hash xor ch.code.toLong()
            hash *= 1099511628211L
        }
        return hash.toULong().toString(16)
    }
}
