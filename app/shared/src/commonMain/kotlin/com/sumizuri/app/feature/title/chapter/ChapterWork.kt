package com.sumizuri.app.feature.title.chapter

import com.sumizuri.app.feature.title.ChapterItem
import kotlin.concurrent.Volatile

// How many results are kept. A few title pages can be open one over another, each with a few results.
private const val Kept = 12

// The slow work on the chapters of a title, such as putting thousands of them in order, kept for the last lists it was done on. Every new state of a title page, such as after a pick or as a download moves on, holds the very same list of chapters, so it finds the work done instead of doing it again. A result is found by that very list and by the choices it depends on.
internal object ChapterWork {
    private class Entry(val chapters: List<ChapterItem>, val key: Any, val value: Any?)

    // Swapped whole on each change, so a reader on another thread always sees a full list. Two threads doing the same work at once only means it is done twice.
    @Volatile
    private var entries: List<Entry> = emptyList()

    @Suppress("UNCHECKED_CAST")
    fun <T> of(chapters: List<ChapterItem>, key: Any, work: () -> T): T {
        entries.firstOrNull { it.chapters === chapters && it.key == key }?.let { return it.value as T }
        val value = work()
        entries = (listOf(Entry(chapters, key, value)) + entries).take(Kept)
        return value
    }
}
