package com.sumizuri.app.data.migration

import androidx.room3.withWriteTransaction
import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.category.TitleCategoryEntity
import com.sumizuri.app.data.db.chapter.ChapterEntity
import com.sumizuri.app.data.db.progress.ProgressEntity
import com.sumizuri.app.data.library.ChapterCarry
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.settings.CoverFocusRepository
import com.sumizuri.app.data.settings.ReaderSettingsRepository
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceEntry

// What moving a title would carry over, so the person can see it before they agree.
data class MigrationPlan(val marked: Int, val matched: Int, val oldChapters: Int, val newChapters: Int)

// Moves a saved title to an entry of another source, or of the same source when the address changed. Read marks, places, bookmarks, the choices of the reader and the spot on the cover follow it, the marks to the chapters that have the same number.
class TitleMigration(
    private val db: AppDatabase,
    private val library: LibraryRepository,
    private val reading: ReaderSettingsRepository,
    private val focus: CoverFocusRepository,
    private val carry: ChapterCarry,
) {
    suspend fun plan(titleId: Long, target: List<SourceChapter>): MigrationPlan {
        val chapters = db.chapters().allOf(titleId)
        val touched = touchedIds(titleId).let { ids -> chapters.filter { it.id in ids || it.bookmarked } }
        // The chapters of the source as they would be saved, so they are paired the way the move will pair them.
        val numbers = chapterNumbers(target)
        val listed = target.mapIndexed { index, chapter ->
            ChapterEntity(
                id = index + 1L,
                titleId = 0,
                url = chapter.url,
                number = numbers[index],
                name = chapter.title,
                publishedAt = 0,
                bookmarked = false,
                season = chapter.season,
            )
        }
        return MigrationPlan(touched.size, matchChapters(touched, listed).size, chapters.size, target.size)
    }

    // What is known about a saved title for finding it elsewhere: its name and author, the numbers of its chapters and the chapter the reader is up to. Null when the title is gone.
    suspend fun subjectOf(titleId: Long): MigrationSubject? {
        val title = db.titles().get(titleId) ?: return null
        val chapters = db.chapters().allOf(titleId)
        val read = db
            .progress()
            .allOf(titleId)
            .filter { it.read }
            .map { it.chapterId }
            .toSet()
        return MigrationSubject(
            title.name,
            title.author.ifBlank { null },
            chapters.map { it.number }.toSet(),
            chapters
                .filter {
                    it.id in
                        read
                }.maxOfOrNull { it.number },
        )
    }

    // Gives the id of the title to open afterwards. Without keepOld the old title is removed. The shelves and the star of the old title go with it unless told not to. It is all written at once or not at all, so a move that is stopped or fails halfway leaves the old title as it was and no second one. Chapters are the ones the new source lists, when they were read already. Without them nothing is asked of the source: the title brings its chapters that hold a mark or a bookmark, and the source's own take their place when the title is next opened or checked. A title that has chapters already, as one moved into, takes the marks at once.
    suspend fun migrate(
        titleId: Long,
        sourceId: String,
        kind: Kind,
        entry: SourceEntry,
        chapters: List<SourceChapter>?,
        keepOld: Boolean,
        keepShelves: Boolean = true,
        keepFavorite: Boolean = true,
    ): Long = db.withWriteTransaction {
        val old = db.titles().get(titleId) ?: return@withWriteTransaction titleId
        if (old.sourceId == sourceId && old.externalId == entry.url) return@withWriteTransaction titleId
        val existing = library.savedId(sourceId, entry)?.let { db.titles().get(it) }
        val newId = library.addFromSource(sourceId, kind, entry, chapters.orEmpty())
        if (existing == null && !old.inLibrary) library.setInLibrary(newId, false)
        // The title keeps the time it was added, so it does not show as new. Moved into a title that was in the library already, it keeps the earlier of the two.
        db.titles().setAddedAt(
            newId,
            if (existing?.inLibrary ==
                true
            ) {
                minOf(existing.addedAt, old.addedAt)
            } else {
                old.addedAt
            },
        )
        val newChapters = db.chapters().allOf(newId)
        if (newChapters.isEmpty()) {
            carry.bring(titleId, newId)
        } else {
            val oldChapters = db.chapters().allOf(titleId)
            val match = matchChapters(oldChapters, newChapters)
            // A chapter that already has a mark in the new title keeps it.
            val taken = db
                .progress()
                .allOf(newId)
                .map { it.chapterId }
                .toSet()
            db.progress().putAll(
                db.progress().allOf(titleId).mapNotNull { mark ->
                    val target = match[mark.chapterId]?.takeIf { it !in taken } ?: return@mapNotNull null
                    ProgressEntity(target, newId, mark.read, mark.readAt, mark.position)
                },
            )
            for (chapter in oldChapters.filter { it.bookmarked }) {
                match[chapter.id]?.let {
                    db.chapters().setBookmarked(
                        it,
                        true,
                    )
                }
            }
        }
        // A removed title takes its choices with it, so none are left behind under its id.
        reading.carryOver(titleId, newId, forget = !keepOld)
        focus.carryOver(titleId, newId, forget = !keepOld)
        if (keepShelves) {
            val shelves = db.categories().categoriesOf(titleId)
            if (shelves.isNotEmpty()) db.categories().addAll(shelves.map { TitleCategoryEntity(newId, it) })
        }
        if (keepFavorite && old.favorite) library.setFavorite(listOf(newId), true)
        if (!keepOld) {
            db.titles().delete(titleId)
            carry.forget(titleId)
        }
        newId
    }

    private suspend fun touchedIds(titleId: Long): Set<Long> = db
        .progress()
        .allOf(titleId)
        .map { it.chapterId }
        .toSet()
}
