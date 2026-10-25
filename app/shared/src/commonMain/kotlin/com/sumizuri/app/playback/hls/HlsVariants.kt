package com.sumizuri.app.playback.hls

import com.sumizuri.app.playback.fetchPlaylist
import com.sumizuri.app.source.model.SourceTrack
import com.sumizuri.app.source.model.SourceVideo

// Whether a video is an HLS stream, which has a playlist of qualities behind its address.
val SourceVideo.isHls: Boolean get() = url
    .substringBefore('?')
    .substringBefore('#')
    .lowercase()
    .endsWith(".m3u8")

// Turns each HLS video that has several qualities into one video for each quality, tallest first, so the quality can be picked from a list and not left to the engine. A quality that keeps its sound in a separate playlist carries that sound with it. A video that cannot be read, or has one quality, is left as it was.
suspend fun expandHlsMasters(videos: List<SourceVideo>): List<SourceVideo> =
    videos.flatMap { video -> if (video.isHls) expand(video) else listOf(video) }

private suspend fun expand(video: SourceVideo): List<SourceVideo> {
    val text = fetchPlaylist(video.url, video.headers) ?: return listOf(video)
    if (!HlsPlaylist.isMaster(text)) return listOf(video)
    val master = HlsPlaylist.parseMaster(text, video.url)
    // One variant for each height, the one with the most bits.
    val byHeight = LinkedHashMap<Any, HlsVariant>()
    for (variant in master.variants) {
        val key: Any = variant.height ?: variant.uri
        val held = byHeight[key]
        if (held == null || (variant.bandwidth ?: 0) > (held.bandwidth ?: 0)) byHeight[key] = variant
    }
    val variants = byHeight.values.sortedByDescending { it.height ?: 0 }
    if (variants.size < 2) return listOf(video)
    val wording = Regex(
        "\\b(softsub|hardsub|sub|dub)\\b",
        RegexOption.IGNORE_CASE,
    ).find(video.quality.orEmpty())?.groupValues?.get(1)
    return variants.mapIndexed { index, variant ->
        val base =
            variant.height?.let { "${it}p" } ?: variant.bandwidth?.let { "${it / 1000} kbps" } ?: "Quality ${index + 1}"
        val audio = HlsPlaylist.audioFor(master, variant)
        video.copy(
            url = variant.uri,
            quality = if (wording == null) base else "$base $wording",
            pairedAudio = audio?.let { SourceTrack(it.uri.orEmpty(), it.name, it.language) },
        )
    }
}
