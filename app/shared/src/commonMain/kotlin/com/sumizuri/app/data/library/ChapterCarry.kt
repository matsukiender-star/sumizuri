package com.sumizuri.app.data.library

import androidx.room3.withWriteTransaction
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.chapter.ChapterEntity
import com.sumizuri.app.data.db.progress.ProgressEntity
import com.sumizuri.app.data.db.setting.SettingEntity
import com.sumizuri.app.data.migration.matchChapters
import com.sumizuri.app.source.model.SourceChapter

// How many rows one statement takes at most.
private const val Chunk = 500

private fun key(titleId: Long) = "chapters.carried.$titleId"

// The chapters a title brought with it when it came from a backup or moved to another source. Only the ones with something of the person's are brought: a read mark, a place or a bookmark. They hold those until the source of the title lists its own chapters, which is asked when the title is opened or the library is checked, so nothing is read from the source while importing or moving. Then each mark and bookmark goes to the chapter of the source that is the same one, and the brought chapters that found theirs are let go.
class ChapterCarry(private val db: AppDatabase) {
    // Says that the chapters of a title are brought ones, to be settled the next time its source lists its chapters.
    suspend fun mark(titleId: Long) = db.settings().put(SettingEntity(key(titleId), "1"))

    // Copies the chapters of one title that hold a mark or a bookmark onto another, with their marks, and says they are brought. Gives how many were brought.
    suspend fun bring(from: Long, to: Long): Int {
        val marks = db.progress().allOf(from)
        val marked = marks.mapTo(HashSet()) { it.chapterId }
        val kept = db.chapters().allOf(from).filter { it.id in marked || it.bookmarked }
        if (kept.isEmpty()) return 0
        val urlOf = kept.associate { it.id to it.url }
        for (part in kept.chunked(Chunk)) db.chapters().insertAll(part.map { it.copy(id = 0, titleId = to) })
        val idByUrl = db.chapters().allOf(to).associate { it.url to it.id }
        db.progress().putAll(
            marks.mapNotNull { mark ->
                urlOf[mark.chapterId]?.let(idByUrl::get)?.let {
                    ProgressEntity(it, to, mark.read, mark.readAt, mark.position)
                }
            },
        )
        mark(to)
        return kept.size
    }

    // Whether the title still holds chapters brought from a backup or another source, whose places the chapters of the source take.
    suspend fun waiting(titleId: Long): Boolean = db.settings().get(key(titleId)) != null

    // Lets the chapters the source listed take the place of the brought ones, once they are saved. A title that brought none is left as it is. Each mark and bookmark goes to the chapter of the source that is the same one, and the brought chapter goes. A brought chapter that holds a mark or a bookmark and finds no chapter stays, so nothing read is lost to a list that numbers its chapters another way, and it is tried again the next time the source lists its chapters.
    suspend fun settle(titleId: Long, listed: List<SourceChapter>) {
        if (listed.isEmpty() || db.settings().get(key(titleId)) == null) return
        db.withWriteTransaction {
            val urls = listed.mapTo(HashSet()) { it.url }
            val (fresh, brought) = db.chapters().allOf(titleId).partition { it.url in urls }
            val pairs = matchChapters(brought, fresh)
            val marks = db.progress().allOf(titleId)
            val broughtIds = brought.mapTo(HashSet()) { it.id }
            // A chapter of the source that has a mark of its own keeps it.
            val taken = marks.filter { it.chapterId !in broughtIds }.mapTo(HashSet()) { it.chapterId }
            db.progress().putAll(
                marks.mapNotNull { mark ->
                    pairs[mark.chapterId]
                        ?.takeIf {
                            it !in taken
                        }?.let { ProgressEntity(it, titleId, mark.read, mark.readAt, mark.position) }
                },
            )
            val bookmarked = brought.filter(ChapterEntity::bookmarked).mapNotNull { pairs[it.id] }
            for (part in bookmarked.chunked(Chunk)) db.chapters().setBookmarkedMany(part, true)
            // The brought chapters that found theirs go, with their marks, and so do the ones that hold nothing.
            val held = marks.mapTo(HashSet()) { it.chapterId }
            val (gone, kept) = brought.partition { it.id in pairs || (it.id !in held && !it.bookmarked) }
            for (part in gone.map { it.id }.chunked(Chunk)) db.chapters().deleteMany(part)
            if (kept.isEmpty()) {
                db.settings().delete(key(titleId))
                if (gone.isNotEmpty()) {
                    AppLog.info(
                        "library",
                        "title $titleId settled ${brought.size} brought chapters, ${pairs.size} found their chapter",
                    )
                }
                return@withWriteTransaction
            }
            // What a kept one and the source call their chapters, so the log shows why they did not pair.
            val example = kept.first().let { "\"${it.name.orEmpty()}\" ${it.number}" }
            val range = if (fresh.isEmpty()) {
                ""
            } else {
                " from ${fresh.minOf(
                    ChapterEntity::number,
                )} to ${fresh.maxOf(ChapterEntity::number)}"
            }
            AppLog.warn(
                "library",
                "title $titleId settled ${gone.size} of ${brought.size} brought chapters, ${kept.size} with a mark found no chapter and stay, such as $example, while the source lists ${fresh.size}$range",
            )
        }
    }

    // Forgets that a title has brought chapters, for a title that is removed.
    suspend fun forget(titleId: Long) = db.settings().delete(key(titleId))
}
