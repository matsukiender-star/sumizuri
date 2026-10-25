package com.sumizuri.app.feature.browse.editor

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_call_failed
import com.sumizuri.app.shared.resources.ext_no_catalog
import com.sumizuri.app.shared.resources.ext_note_empty
import com.sumizuri.app.shared.resources.ext_open_list_first
import com.sumizuri.app.shared.resources.ext_report_count
import com.sumizuri.app.shared.resources.ext_report_error
import com.sumizuri.app.shared.resources.ext_report_no_chapter
import com.sumizuri.app.shared.resources.ext_report_no_list
import com.sumizuri.app.shared.resources.ext_report_no_title
import com.sumizuri.app.shared.resources.ext_report_not_offered
import com.sumizuri.app.shared.resources.ext_report_ok
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.model.CommentSort
import com.sumizuri.app.source.model.SourceCatalog
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

// The pages of a chapter and the comments that are listed, and how much of a page of words is shown.
private const val PagesShown = 30
private const val CommentsShown = 50
private const val WordsShown = 160

// What a step gave: the view that shows it, how many things came back, and what looks wrong in them.
internal class StepResult(val view: TestView, val count: Int?, val notes: List<TestNote>)

// The calls of each step of a test, on the draft that is open, keeping what came back in the state the test pages show.
internal class TestSteps(private val state: MutableStateFlow<ExtensionEditorState>) {
    private val test: ExtensionTest get() = state.value.test

    private fun update(change: (ExtensionTest) -> ExtensionTest) = state.update { it.copy(test = change(it.test)) }

    fun info(source: Source): StepResult {
        update { it.copy(info = source.info) }
        return StepResult(TestView.Info, null, emptyList())
    }

    suspend fun fetch(
        source: Source,
        step: TestStep,
        query: String,
        catalog: SourceCatalog?,
        page: Int,
    ): List<SourceEntry> = when (step) {
        TestStep.Popular -> source.popular(page)
        TestStep.Latest -> source.latest(page)
        TestStep.Search -> source.search(query, page)
        else -> source.catalog(catalog?.id ?: error(getString(Res.string.ext_no_catalog)), page)
    }.orThrow()

    suspend fun list(source: Source, step: TestStep, query: String, catalog: SourceCatalog?): StepResult {
        val entries = fetch(source, step, query, catalog, 1)
        update {
            it.copy(
                list = step,
                query = query,
                catalog = catalog,
                page = 1,
                entries = entries,
                ended = entries.isEmpty(),
            )
        }
        return StepResult(TestView.List, entries.size, listNotes(entries))
    }

    // The next page of the list that is open. A page that is empty or the same as the one before ends the list, as it would in the app.
    suspend fun nextPage(source: Source): StepResult {
        val now = test
        val step = now.list ?: error(getString(Res.string.ext_open_list_first))
        val page = now.page + 1
        val added = fetch(source, step, now.query, now.catalog, page)
        val known = now.entries.map { it.url }.toSet()
        val fresh = added.filter { it.url !in known }
        update { it.copy(page = page, entries = it.entries + fresh, ended = fresh.isEmpty()) }
        return StepResult(TestView.List, added.size, nextPageNotes(now.entries, added, page))
    }

    // A title of the list: its details, then its chapters, as its page in the app would read them. Code without details of its own has the title as the list gave it, as in the app.
    suspend fun title(source: Source, entry: SourceEntry): StepResult {
        val details = if (source.info.hasDetails) source.details(entry).orThrow() else entry
        val shown = details.copy(
            url = entry.url,
            title = details.title.ifBlank { entry.title },
            coverUrl =
            details.coverUrl ?: entry.coverUrl,
        )
        update {
            it.copy(
                entry = shown,
                chapters = emptyList(),
                chapter = null,
                pages = emptyList(),
                pageCount = 0,
                comments = emptyList(),
                chapterComments = emptyList(),
            )
        }
        val chapters = source.chapters(entry).orThrow()
        update { it.copy(chapters = chapters) }
        return StepResult(TestView.Title, chapters.size, titleNotes(details.copy(coverUrl = shown.coverUrl), chapters))
    }

    // A chapter: the addresses of its pictures, the start of its words, or its videos with their quality.
    suspend fun chapter(source: Source, chapter: SourceChapter): StepResult {
        if (state.value.kind == Kind.Anime) {
            val videos = source.videos(chapter).orThrow()
            val lines = videos.map { video -> PageLine(listOfNotNull(video.quality, video.url).joinToString("  ")) }
            update {
                it.copy(
                    chapter = chapter,
                    pages = lines.take(PagesShown),
                    pageCount = videos.size,
                    chapterComments = emptyList(),
                )
            }
            return StepResult(TestView.Chapter, videos.size, videoNotes(videos) + streamNotes(videos.firstOrNull()))
        }
        val pages = source.pages(chapter).orThrow()
        val lines = pages.map { page -> PageLine(page.imageUrl ?: page.text.orEmpty().take(WordsShown), page.imageUrl) }
        update {
            it.copy(
                chapter = chapter,
                pages = lines.take(PagesShown),
                pageCount = pages.size,
                chapterComments = emptyList(),
            )
        }
        return StepResult(TestView.Chapter, pages.size, pageNotes(pages))
    }

    suspend fun comments(source: Source, entry: SourceEntry, sort: CommentSort): StepResult {
        val found = source.comments(entry, sort).orThrow().comments
        update { it.copy(comments = found.take(CommentsShown), commentSort = sort) }
        return StepResult(TestView.Comments, found.size, commentNotes(found))
    }

    suspend fun chapterComments(source: Source, chapter: SourceChapter, sort: CommentSort): StepResult {
        val found = source.chapterComments(chapter, sort).orThrow().comments
        update { it.copy(chapterComments = found.take(CommentsShown), commentSort = sort) }
        return StepResult(TestView.ChapterComments, found.size, commentNotes(found))
    }

    suspend fun filters(source: Source): StepResult {
        val groups = source.filters().orThrow()
        update { it.copy(filters = groups) }
        return StepResult(TestView.Filters, groups.size, emptyNote(groups))
    }

    suspend fun catalogs(source: Source): StepResult {
        val found = source.catalogs().orThrow()
        update { it.copy(catalogs = found) }
        return StepResult(TestView.Catalogs, found.size, emptyNote(found))
    }

    // The settings the source offers, with the values it has now.
    suspend fun settings(source: Source): StepResult {
        val found = source.sourcePreferences().orThrow()
        val values = source.preferences()
        update { it.copy(settings = found, settingValues = values) }
        return StepResult(TestView.Settings, found.size, emptyNote(found))
    }

    private fun emptyNote(found: List<Any>) =
        if (found.isEmpty()) listOf(TestNote(Res.string.ext_note_empty)) else emptyList()

    // The first words of a title, which a search of the site should find it by.
    fun searchWords(title: String) = title
        .split(' ')
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString(" ")
}

// The value of an answer, or its message as an error, so a test stops at the first call that fails.
internal fun <T> SourceResult<T>.orThrow(): T = when (this) {
    is SourceResult.Ok -> value
    is SourceResult.Err -> throw IllegalStateException(error.message)
}
