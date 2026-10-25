package com.sumizuri.app.feature.reader.view

import com.sumizuri.app.feature.reader.ReaderPage

// A page taller than this many times its width reads as a piece of a long strip.
private const val StripAspect = 2.2

// A page this tall is a strip even when most of the others were cut shorter.
private const val VeryTall = 3.0

// If this share of the pages is very tall the chapter is a strip.
private const val VeryTallShare = 0.4

// How many pages are looked at.
private const val SampleCount = 5

// Says whether a chapter is a long strip, from the heights over the widths of some of its pages. The middle one counts, so a short page of introduction or a banner at the ends does not decide it.
fun looksLikeStrip(aspects: List<Double>): Boolean {
    if (aspects.isEmpty()) return false
    val sorted = aspects.sorted()
    val middle = if (sorted.size % 2 ==
        1
    ) {
        sorted[sorted.size / 2]
    } else {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
    }
    return middle >= StripAspect || aspects.count { it >= VeryTall } >= aspects.size * VeryTallShare
}

// The addresses of the pages to look at, spread through the chapter. The first and the last are left out when there are enough pages, since they are often a short introduction or a banner.
fun sampleUrls(pages: List<ReaderPage>): List<String> {
    val urls = pages.mapNotNull { it.imageUrl }
    val inside = if (urls.size >= 4) urls.subList(1, urls.size - 1) else urls
    if (inside.size <= SampleCount) return inside
    return List(SampleCount) { n -> inside[n * (inside.size - 1) / (SampleCount - 1)] }
}
