package com.sumizuri.app.feature.browse.editor

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_note_details_no_title
import com.sumizuri.app.shared.resources.ext_note_empty
import com.sumizuri.app.shared.resources.ext_note_no_address
import com.sumizuri.app.shared.resources.ext_note_no_cover
import com.sumizuri.app.shared.resources.ext_note_no_number
import com.sumizuri.app.shared.resources.ext_note_no_picture
import com.sumizuri.app.shared.resources.ext_note_no_text
import com.sumizuri.app.shared.resources.ext_note_no_title
import com.sumizuri.app.shared.resources.ext_note_no_videos
import com.sumizuri.app.shared.resources.ext_note_page_empty
import com.sumizuri.app.shared.resources.ext_note_page_overlap
import com.sumizuri.app.shared.resources.ext_note_page_repeats
import com.sumizuri.app.shared.resources.ext_note_same_address
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceComment
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.model.SourcePage
import com.sumizuri.app.source.model.SourceVideo

// Looks over what the code gave and says what would look wrong in the app, such as titles with no cover, which the person would otherwise only see later.

internal fun listNotes(entries: List<SourceEntry>): List<TestNote> = buildList {
    if (entries.isEmpty()) {
        add(TestNote(Res.string.ext_note_empty))
        return@buildList
    }
    val total = entries.size
    count(entries.count { it.title.isBlank() }, total, Res.string.ext_note_no_title)
    count(entries.count { it.url.isBlank() }, total, Res.string.ext_note_no_address)
    // The app keeps a cover only when it is a full address, so one the page gave as /images/a.jpg is lost here.
    count(entries.count { it.coverUrl == null }, total, Res.string.ext_note_no_cover)
    repeated(entries.map { it.url })
}

// What a further page of a list adds: nothing, the same titles as the page before, or some that were there already.
internal fun nextPageNotes(before: List<SourceEntry>, added: List<SourceEntry>, page: Int): List<TestNote> = buildList {
    if (added.isEmpty()) {
        add(TestNote(Res.string.ext_note_page_empty, listOf(page)))
        return@buildList
    }
    val known = before.map { it.url }.toSet()
    val again = added.count { it.url in known }
    when {
        again == added.size -> add(TestNote(Res.string.ext_note_page_repeats, listOf(page, page - 1)))
        again > 0 -> add(TestNote(Res.string.ext_note_page_overlap, listOf(again, page)))
    }
    addAll(listNotes(added).filter { it.text != Res.string.ext_note_empty })
}

internal fun titleNotes(entry: SourceEntry, chapters: List<SourceChapter>): List<TestNote> = buildList {
    if (entry.title.isBlank()) add(TestNote(Res.string.ext_note_details_no_title))
    if (entry.coverUrl == null) add(TestNote(Res.string.ext_note_no_cover, listOf(1, 1)))
    if (chapters.isEmpty()) {
        add(TestNote(Res.string.ext_note_empty))
        return@buildList
    }
    val total = chapters.size
    count(chapters.count { it.title.isBlank() }, total, Res.string.ext_note_no_title)
    count(chapters.count { it.number == null }, total, Res.string.ext_note_no_number)
    repeated(chapters.map { it.url })
}

internal fun pageNotes(pages: List<SourcePage>): List<TestNote> = buildList {
    if (pages.isEmpty()) {
        add(TestNote(Res.string.ext_note_empty))
        return@buildList
    }
    count(pages.count { it.imageUrl == null && it.text.isNullOrBlank() }, pages.size, Res.string.ext_note_no_picture)
}

internal fun videoNotes(videos: List<SourceVideo>): List<TestNote> =
    if (videos.isEmpty()) listOf(TestNote(Res.string.ext_note_no_videos)) else emptyList()

internal fun commentNotes(comments: List<SourceComment>): List<TestNote> = buildList {
    if (comments.isEmpty()) {
        add(TestNote(Res.string.ext_note_empty))
        return@buildList
    }
    count(comments.count { it.text.isBlank() }, comments.size, Res.string.ext_note_no_text)
}

private fun MutableList<TestNote>.count(found: Int, total: Int, text: org.jetbrains.compose.resources.StringResource) {
    if (found > 0) add(TestNote(text, listOf(found, total)))
}

// Addresses that are listed more than once, which the app would show once.
private fun MutableList<TestNote>.repeated(addresses: List<String>) {
    val twice = addresses.size - addresses.distinct().size
    if (twice > 0) add(TestNote(Res.string.ext_note_same_address, listOf(twice)))
}
