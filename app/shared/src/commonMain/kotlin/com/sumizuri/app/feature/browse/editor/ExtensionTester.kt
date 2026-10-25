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

// Asks the draft for each thing a source can give and shows what came back, with notes on what would look wrong in the app. One step runs at a time, and a new one stops the one before. Run all goes through every step the code offers and writes a report.
internal class ExtensionTester(
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ExtensionEditorState>,
    private val draft: suspend () -> Source,
    private val label: String,
) {
    private var job: Job? = null

    // The calls of each step, which keep what came back in the state.
    private val steps = TestSteps(state)

    private val test: ExtensionTest get() = state.value.test

    private fun update(change: (ExtensionTest) -> ExtensionTest) = state.update { it.copy(test = change(it.test)) }

    fun stop() {
        job?.cancel()
        job = null
        update { it.copy(running = null, runningAll = false) }
    }

    fun back() = update { it.back() }
    fun show(view: TestView) = update { it.showing(view) }

    fun info() = single(TestStep.Info) { steps.info(it) }
    fun popular() = single(TestStep.Popular) { steps.list(it, TestStep.Popular, "", null) }
    fun latest() = single(TestStep.Latest) { steps.list(it, TestStep.Latest, "", null) }
    fun search(query: String) {
        val words = query.trim()
        if (words.isNotEmpty()) single(TestStep.Search) { steps.list(it, TestStep.Search, words, null) }
    }
    fun catalog(catalog: SourceCatalog) = single(TestStep.Catalog) { steps.list(it, TestStep.Catalog, "", catalog) }
    fun nextPage() = single(TestStep.NextPage) { steps.nextPage(it) }
    fun open(entry: SourceEntry) = single(TestStep.Details) { steps.title(it, entry) }
    fun open(chapter: SourceChapter) = single(chapterStep()) { steps.chapter(it, chapter) }
    fun comments(sort: CommentSort) =
        test.entry?.let { entry -> single(TestStep.Comments) { steps.comments(it, entry, sort) } }
    fun chapterComments(sort: CommentSort) =
        test.chapter?.let { chapter -> single(TestStep.ChapterComments) { steps.chapterComments(it, chapter, sort) } }
    fun filters() = single(TestStep.Filters) { steps.filters(it) }
    fun catalogs() = single(TestStep.Catalogs) { steps.catalogs(it) }
    fun settings() = single(TestStep.Settings) { steps.settings(it) }

    // A step of the editor's own, such as taking the details from the code, which shows nothing in the results.
    fun custom(step: TestStep, block: suspend (Source) -> Unit) = single(step) {
        block(it)
        null
    }

    private fun chapterStep() = if (state.value.kind == Kind.Anime) TestStep.Videos else TestStep.Pages

    // Runs one step on the draft as the code is now, and says how long it took or what went wrong.
    private fun single(step: TestStep, block: suspend (Source) -> StepResult?) {
        job?.cancel()
        job = scope.launch {
            update {
                it.copy(
                    running = step,
                    runningAll = false,
                    error = null,
                    failed = null,
                    done = null,
                    tookMs = null,
                )
            }
            val start = nowMillis()
            try {
                AppLog.info(label, "${step.name.lowercase()} started")
                val result = block(draft())
                val took = nowMillis() - start
                AppLog.info(label, "${step.name.lowercase()} done in $took ms")
                update { now ->
                    (
                        if (result ==
                            null
                        ) {
                            now
                        } else {
                            now.showing(result.view).copy(notes = result.notes)
                        }
                        ).copy(running = null, done = step, tookMs = took)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                val message = error.message ?: getString(Res.string.ext_call_failed)
                AppLog.warn(label, "${step.name.lowercase()} failed, $message")
                update {
                    it.copy(
                        running = null,
                        failed = step,
                        error = message,
                        notes = emptyList(),
                        tookMs =
                        nowMillis() - start,
                    )
                }
            }
        }
    }

    // Every step in turn, each on what the one before found: the lists, a further page, a search for the first title, that title with its chapters, the first chapter, then the comments, filters, catalogs and settings when the code says it has them. A step that fails does not stop the rest.
    fun runAll() {
        job?.cancel()
        job = scope.launch {
            update {
                it
                    .copy(
                        running = TestStep.Start,
                        runningAll = true,
                        error = null,
                        failed = null,
                        done = null,
                        tookMs = null,
                        notes = emptyList(),
                        report = emptyList(),
                    ).showing(TestView.Report)
            }
            val started = nowMillis()
            AppLog.info(label, "run all started")
            var opened: Source? = null
            step(TestStep.Start) { opened = draft() }
            val source = opened
            if (source != null) {
                val info = source.info
                step(TestStep.Info) { steps.info(source) }
                step(TestStep.Popular) { steps.list(source, TestStep.Popular, "", null) }
                val popular = test.entries
                step(TestStep.Latest) { steps.list(source, TestStep.Latest, "", null) }
                // The list that has titles is the one read on and opened from.
                if (test.entries.isEmpty() &&
                    popular.isNotEmpty()
                ) {
                    update {
                        it.copy(
                            list = TestStep.Popular,
                            query = "",
                            catalog = null,
                            page = 1,
                            entries = popular,
                            ended = false,
                        )
                    }
                }
                val picked = test.entries.firstOrNull()
                if (picked == null) {
                    skip(TestStep.NextPage, ext(Res.string.ext_report_no_list))
                    skip(TestStep.Search, ext(Res.string.ext_report_no_title))
                    skip(TestStep.Details, ext(Res.string.ext_report_no_title))
                } else {
                    step(TestStep.NextPage) { steps.nextPage(source) }
                    step(TestStep.Search) { steps.list(source, TestStep.Search, steps.searchWords(picked.title), null) }
                    step(TestStep.Details) { steps.title(source, picked) }
                }
                val entry = test.entry
                val chapter = test.chapters.firstOrNull()
                if (chapter == null) {
                    skip(
                        chapterStep(),
                        ext(
                            if (entry ==
                                null
                            ) {
                                Res.string.ext_report_no_title
                            } else {
                                Res.string.ext_report_no_chapter
                            },
                        ),
                    )
                } else {
                    step(chapterStep()) { steps.chapter(source, chapter) }
                }
                when {
                    !info.hasComments -> skip(TestStep.Comments, ext(Res.string.ext_report_not_offered))
                    entry == null -> skip(TestStep.Comments, ext(Res.string.ext_report_no_title))
                    else -> step(TestStep.Comments) { steps.comments(source, entry, CommentSort.Newest) }
                }
                when {
                    !info.hasChapterComments -> skip(TestStep.ChapterComments, ext(Res.string.ext_report_not_offered))

                    test.chapter == null -> skip(TestStep.ChapterComments, ext(Res.string.ext_report_no_chapter))

                    else -> test.chapter?.let { shown ->
                        step(TestStep.ChapterComments) {
                            steps.chapterComments(source, shown, CommentSort.Newest)
                        }
                    }
                }
                if (info.hasFilters) {
                    step(TestStep.Filters) {
                        steps.filters(source)
                    }
                } else {
                    skip(TestStep.Filters, ext(Res.string.ext_report_not_offered))
                }
                if (info.hasCatalogs) {
                    step(TestStep.Catalogs) { steps.catalogs(source) }
                    test.catalogs.firstOrNull()?.let { first ->
                        step(
                            TestStep.Catalog,
                        ) { steps.list(source, TestStep.Catalog, "", first) }
                    }
                } else {
                    skip(TestStep.Catalogs, ext(Res.string.ext_report_not_offered))
                }
                if (info.hasPreferences) {
                    step(TestStep.Settings) {
                        steps.settings(source)
                    }
                } else {
                    skip(TestStep.Settings, ext(Res.string.ext_report_not_offered))
                }
            }
            val took = nowMillis() - started
            AppLog.info(label, "run all done in $took ms")
            update {
                it
                    .copy(
                        running = null,
                        runningAll = false,
                        done = TestStep.RunAll,
                        tookMs = took,
                    ).showing(TestView.Report)
            }
        }
    }

    // One step of Run all, written into the report: worked, worked with something to look at, or failed with what went wrong.
    private suspend fun <T> step(step: TestStep, block: suspend () -> T): T? {
        update { it.copy(running = step) }
        val start = nowMillis()
        return try {
            AppLog.info(label, "${step.name.lowercase()} started")
            val value = block()
            val took = nowMillis() - start
            val result = value as? StepResult
            val notes = result?.notes.orEmpty()
            val detail =
                result?.count?.let { TestNote(Res.string.ext_report_count, listOf(it)) }
                    ?: ext(Res.string.ext_report_ok)
            report(ReportLine(step, if (notes.isEmpty()) Outcome.Passed else Outcome.Warned, detail, took, notes))
            AppLog.info(label, "${step.name.lowercase()} done in $took ms")
            value
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val message = error.message ?: getString(Res.string.ext_call_failed)
            AppLog.warn(label, "${step.name.lowercase()} failed, $message")
            report(
                ReportLine(
                    step,
                    Outcome.Failed,
                    TestNote(Res.string.ext_report_error, listOf(message)),
                    nowMillis() - start,
                ),
            )
            null
        }
    }

    private fun skip(step: TestStep, why: TestNote) = report(ReportLine(step, Outcome.Skipped, why))

    private fun report(line: ReportLine) = update { it.copy(report = it.report + line) }

    private fun ext(text: org.jetbrains.compose.resources.StringResource) = TestNote(text)
}
