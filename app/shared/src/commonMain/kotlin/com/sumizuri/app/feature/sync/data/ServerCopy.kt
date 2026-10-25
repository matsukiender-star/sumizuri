package com.sumizuri.app.feature.sync.data

import com.sumizuri.app.data.backup.ImportedChapter
import com.sumizuri.app.data.backup.ImportedTitle

// One chapter of the server's copy, with the title it belongs to and the time that matters for the list it is in.
data class CopyChapter(val title: ImportedTitle, val chapter: ImportedChapter, val at: Long)

// The server's side of this profile's library, opened on the device: when it last changed, and the lists the tabs show.
class ServerCopy(val at: Long, titles: List<ImportedTitle>) {
    // The titles in the library, by name.
    val library: List<ImportedTitle> = titles.filter { it.inLibrary }.sortedBy { it.name.lowercase() }

    // The chapters read, newest first.
    val history: List<CopyChapter> = titles
        .flatMap { title ->
            title.chapters.mapNotNull { chapter ->
                chapter.readAt
                    ?.takeIf {
                        chapter.read
                    }?.let { CopyChapter(title, chapter, it) }
            }
        }.sortedByDescending { it.at }
        .take(ListLimit)

    // The chapters of library titles not read yet, newest upload first.
    val updates: List<CopyChapter> = library
        .flatMap { title ->
            title.chapters
                .filter {
                    !it.read && it.uploadedAt > 0
                }.map { CopyChapter(title, it, it.uploadedAt) }
        }.sortedByDescending { it.at }
        .take(ListLimit)

    companion object {
        // The most rows a list of chapters shows, so a big library stays quick to draw.
        const val ListLimit = 300
    }
}
