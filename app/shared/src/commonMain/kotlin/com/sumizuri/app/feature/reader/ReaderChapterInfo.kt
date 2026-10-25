package com.sumizuri.app.feature.reader

import com.sumizuri.app.data.model.ChapterPlace
import com.sumizuri.app.source.engine.PackedChapterMark

// The stretch of pages a chapter takes in the scroll, with what the bars say about it.
internal fun segmentOf(here: ChapterPlace, start: Int, count: Int, base: String) = ReaderSegment(
    chapterId = here.id,
    start = start,
    count = count,
    name = here.name?.takeIf { it.isNotBlank() },
    number = here.number,
    previousId = here.previousId,
    nextId = here.nextId,
    webUrl = webAddress(here.url, base),
)

// The chapter in view, as far as the bars are concerned.
internal fun ReaderState.activate(segment: ReaderSegment) = copy(
    chapterId = segment.chapterId,
    chapterName = segment.name,
    chapterNumber = segment.number,
    previousId = segment.previousId,
    nextId = segment.nextId,
    webUrl = segment.webUrl,
)

// The chapter address as a web page, which a source may give in full or as a path on its own site.
internal fun webAddress(url: String, base: String): String? = when {
    // A chapter of an extension source is not a web address, it holds the title and the chapter.
    url.contains(PackedChapterMark) -> null

    url.startsWith("http://") || url.startsWith("https://") -> url

    base.isNotBlank() -> base.trimEnd('/') + "/" + url.trimStart('/')

    else -> null
}
