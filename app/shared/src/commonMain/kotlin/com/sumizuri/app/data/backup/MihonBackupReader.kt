package com.sumizuri.app.data.backup

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.readChapterName
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.gzip
import okio.use

// Reads a Mihon backup, a gzip file of protobuf. The titles that are in the library come with their chapters, their reading history and the shelves they were on.
fun readMihonBackup(file: Path, system: FileSystem = FileSystem.SYSTEM): ImportedBackup {
    val top = try {
        ProtoMessage.decode(
            system
                .source(file)
                .gzip()
                .buffer()
                .use { it.readByteArray() },
        )
    } catch (error: Throwable) {
        throw BackupUnreadable(BackupProblem.NotBackup, error)
    }
    try {
        val sources = top
            .messages(SourceList)
            .mapNotNull { source ->
                source.number(SourceId)?.let { id ->
                    source.text(SourceName)?.let {
                        id to
                            it
                    }
                }
            }.toMap()
        val shelves = top.messages(CategoryList).mapNotNull { category ->
            category.text(CategoryName)?.let {
                it.trim() to
                    (category.number(CategoryOrder) ?: 0L)
            }
        }
        val shelfByOrder = shelves.associate { (name, order) -> order to name }
        val titles = top
            .messages(MangaList)
            // A title that is not a favourite is only a title that was read once, and is not in the library.
            .filter { it.number(Favorite) != 0L }
            .map { manga ->
                val history = manga
                    .messages(HistoryList)
                    .mapNotNull { entry ->
                        entry.text(HistoryUrl)?.let { url ->
                            entry
                                .number(HistoryTime)
                                ?.takeIf {
                                    it >
                                        0
                                }?.let { url to it }
                        }
                    }.toMap()
                ImportedTitle(
                    name = manga.text(MangaTitle).orEmpty().trim(),
                    url = manga.text(MangaUrl).orEmpty(),
                    coverUrl = manga.text(MangaCover),
                    kind = Kind.Manga,
                    favorite = true,
                    sourceName = manga.number(MangaSource)?.let { sources[it] },
                    sourceUrl = null,
                    shelves = manga.numbers(MangaCategories).mapNotNull { shelfByOrder[it] },
                    chapters = chaptersOf(manga, history),
                )
            }
        return ImportedBackup(BackupApp.Mihon, titles, shelves.map { ImportedShelf(it.first) })
    } catch (error: Throwable) {
        throw BackupUnreadable(BackupProblem.Damaged, error)
    }
}

// The chapters of a title, with copies of one chapter folded into one, keeping the one that was read. Copies share the number and the volume, piece and extra their names give, so the parts of a split chapter and the same number in two volumes stay apart.
private fun chaptersOf(manga: ProtoMessage, history: Map<String, Long>): List<ImportedChapter> {
    val byPlace = LinkedHashMap<List<Any?>, ImportedChapter>()
    val unnumbered = ArrayList<ImportedChapter>()
    for (chapter in manga.messages(ChapterList)) {
        val url = chapter.text(ChapterUrl).orEmpty()
        val read = (chapter.number(ChapterRead) ?: 0L) != 0L
        // Mihon keeps the number as a 32-bit number, which as it is gives 90.0999984 for 90.1. Its shortest written form is the number the site gave. Mihon writes -1 for a chapter it found no number for, and then the name may still have one.
        val name = chapter.text(ChapterName)?.trim()?.takeIf { it.isNotEmpty() }
        val number = chapter
            .float(ChapterNumber)
            ?.toString()
            ?.toDoubleOrNull()
            ?.takeIf { it >= 0 } ?: name?.let(::chapterNumberFromName)
        val made = ImportedChapter(
            url = url,
            number = number ?: 0.0,
            name = name,
            uploadedAt = (chapter.number(ChapterDate) ?: 0L).coerceAtLeast(0),
            read = read,
            readAt = if (read) history[url] else null,
            // Mihon keeps the page the reader was on and not how many pages there are, so there is no fraction to give.
            position = null,
            bookmarked = (chapter.number(ChapterBookmark) ?: 0L) != 0L,
        )
        if (number == null) {
            unnumbered += made
            continue
        }
        val place = readChapterName(name).let { listOf(number, it.volume, it.piece, it.extra) }
        val existing = byPlace[place]
        if (existing == null || (!existing.read && made.read)) byPlace[place] = made
    }
    return byPlace.values.toList() + unnumbered
}

private const val MangaList = 1
private const val CategoryList = 2
private const val SourceList = 101
private const val MangaSource = 1
private const val MangaUrl = 2
private const val MangaTitle = 3
private const val MangaCover = 9
private const val ChapterList = 16
private const val MangaCategories = 17
private const val Favorite = 100
private const val HistoryList = 104
private const val HistoryUrl = 1
private const val HistoryTime = 2
private const val ChapterUrl = 1
private const val ChapterName = 2
private const val ChapterRead = 4
private const val ChapterBookmark = 5
private const val ChapterDate = 8
private const val ChapterNumber = 9
private const val SourceName = 1
private const val SourceId = 2
private const val CategoryName = 1
private const val CategoryOrder = 2
