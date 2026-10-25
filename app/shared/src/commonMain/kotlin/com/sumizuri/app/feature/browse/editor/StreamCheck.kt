package com.sumizuri.app.feature.browse.editor

import com.sumizuri.app.playback.fetchBytes
import com.sumizuri.app.playback.fetchPlaylist
import com.sumizuri.app.playback.hls.HlsPlaylist
import com.sumizuri.app.playback.hls.isHls
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_note_stream_closed
import com.sumizuri.app.shared.resources.ext_note_stream_key_closed
import com.sumizuri.app.shared.resources.ext_note_stream_key_size
import com.sumizuri.app.shared.resources.ext_note_stream_no_parts
import com.sumizuri.app.shared.resources.ext_note_stream_part_closed
import com.sumizuri.app.source.model.SourceVideo

// The size of the key of an AES-128 stream, the only size a player can unlock it with.
private const val KeyBytes = 16

// Opens the stream of a video the way the player would and says what would stop it, such as a key no player can use. Only HLS streams are looked into, since a plain file is opened as it is.
internal suspend fun streamNotes(video: SourceVideo?): List<TestNote> {
    if (video == null || !video.isHls) return emptyList()
    val headers = video.headers
    var address = video.url
    var text = fetchPlaylist(address, headers) ?: return listOf(TestNote(Res.string.ext_note_stream_closed))
    if (HlsPlaylist.isMaster(text)) {
        val variant = HlsPlaylist.parseMaster(text, address).variants.maxByOrNull { it.height ?: 0 }
            ?: return listOf(TestNote(Res.string.ext_note_stream_no_parts))
        address = variant.uri
        text = fetchPlaylist(address, headers) ?: return listOf(TestNote(Res.string.ext_note_stream_closed))
    }
    val lines = text.lines().map { it.trim() }
    val part = lines.firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
        ?: return listOf(TestNote(Res.string.ext_note_stream_no_parts))
    keyNote(lines, address, headers)?.let { return listOf(it) }
    if (fetchBytes(HlsPlaylist.resolve(address, part), headers) == null) {
        return listOf(TestNote(Res.string.ext_note_stream_part_closed))
    }
    return emptyList()
}

// The key of a stream locked with AES-128 must be fetched and be 16 bytes, or no player can unlock its parts.
private suspend fun keyNote(lines: List<String>, base: String, headers: Map<String, String>): TestNote? {
    val tag = lines.firstOrNull { it.startsWith("#EXT-X-KEY:") && "METHOD=AES-128" in it } ?: return null
    val uri = Regex("URI=\"([^\"]+)\"").find(tag)?.groupValues?.get(1) ?: return null
    val key =
        fetchBytes(HlsPlaylist.resolve(base, uri), headers) ?: return TestNote(Res.string.ext_note_stream_key_closed)
    return if (key.size == KeyBytes) null else TestNote(Res.string.ext_note_stream_key_size, listOf(key.size))
}
