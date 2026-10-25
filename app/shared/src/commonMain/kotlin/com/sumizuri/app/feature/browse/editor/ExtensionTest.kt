package com.sumizuri.app.feature.browse.editor

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_edit_step_catalog
import com.sumizuri.app.shared.resources.ext_edit_step_catalogs
import com.sumizuri.app.shared.resources.ext_edit_step_chapter_comments
import com.sumizuri.app.shared.resources.ext_edit_step_comments
import com.sumizuri.app.shared.resources.ext_edit_step_details
import com.sumizuri.app.shared.resources.ext_edit_step_filters
import com.sumizuri.app.shared.resources.ext_edit_step_info
import com.sumizuri.app.shared.resources.ext_edit_step_next
import com.sumizuri.app.shared.resources.ext_edit_step_pages
import com.sumizuri.app.shared.resources.ext_edit_step_run_all
import com.sumizuri.app.shared.resources.ext_edit_step_search
import com.sumizuri.app.shared.resources.ext_edit_step_settings
import com.sumizuri.app.shared.resources.ext_edit_step_start
import com.sumizuri.app.shared.resources.ext_edit_step_videos
import com.sumizuri.app.shared.resources.listing_latest
import com.sumizuri.app.shared.resources.listing_popular
import com.sumizuri.app.source.model.CommentSort
import com.sumizuri.app.source.model.FilterGroup
import com.sumizuri.app.source.model.SourceCatalog
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceComment
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.model.SourceInfo
import com.sumizuri.app.source.model.SourcePreference
import org.jetbrains.compose.resources.StringResource

// One thing the editor asks of the draft, named for the screen and the log.
enum class TestStep(val label: StringResource) {
    Start(Res.string.ext_edit_step_start),
    Info(Res.string.ext_edit_step_info),
    Popular(Res.string.listing_popular),
    Latest(Res.string.listing_latest),
    Search(Res.string.ext_edit_step_search),
    Catalog(Res.string.ext_edit_step_catalog),
    NextPage(Res.string.ext_edit_step_next),
    Details(Res.string.ext_edit_step_details),
    Pages(Res.string.ext_edit_step_pages),
    Videos(Res.string.ext_edit_step_videos),
    Comments(Res.string.ext_edit_step_comments),
    ChapterComments(Res.string.ext_edit_step_chapter_comments),
    Filters(Res.string.ext_edit_step_filters),
    Catalogs(Res.string.ext_edit_step_catalogs),
    Settings(Res.string.ext_edit_step_settings),
    RunAll(Res.string.ext_edit_step_run_all),
}

// What the results show.
enum class TestView { Hint, Info, List, Title, Chapter, Comments, ChapterComments, Filters, Catalogs, Settings, Report }

// Something worth a look in what the code gave, such as titles with no cover, said with a text and the numbers in it.
data class TestNote(val text: StringResource, val args: List<Any> = emptyList())

// One line of the report of Run all: the step, whether it worked, gave something to look at, failed or was not tried, what it gave and how long it took.
data class ReportLine(
    val step: TestStep,
    val outcome: Outcome,
    val detail: TestNote,
    val ms: Long? = null,
    val notes: List<TestNote> = emptyList(),
)

enum class Outcome { Passed, Warned, Failed, Skipped }

// A page of a chapter as the test lists it: a picture's address, the start of its words, or a video with what it brings.
data class PageLine(val text: String, val imageUrl: String? = null)

// Where the tests are. The views that were opened one from another are kept as a trail, so back leads to the one before, and what each view shows is kept while others are looked at.
data class ExtensionTest(
    val running: TestStep? = null,
    // Run all is going, and running names the step of it that runs now.
    val runningAll: Boolean = false,
    val trail: List<TestView> = emptyList(),
    val info: SourceInfo? = null,
    // The list: which step gave it, the words or the catalog it was for, the pages read so far and whether the last one was empty.
    val list: TestStep? = null,
    val query: String = "",
    val catalog: SourceCatalog? = null,
    val page: Int = 1,
    val entries: List<SourceEntry> = emptyList(),
    val ended: Boolean = false,
    val entry: SourceEntry? = null,
    val chapters: List<SourceChapter> = emptyList(),
    val chapter: SourceChapter? = null,
    val pages: List<PageLine> = emptyList(),
    // How many pages the chapter has, of which only the first are listed.
    val pageCount: Int = 0,
    // The comments of the title and of the chapter, in the order asked for.
    val comments: List<SourceComment> = emptyList(),
    val chapterComments: List<SourceComment> = emptyList(),
    val commentSort: CommentSort = CommentSort.Newest,
    val filters: List<FilterGroup> = emptyList(),
    val catalogs: List<SourceCatalog> = emptyList(),
    val settings: List<SourcePreference> = emptyList(),
    val settingValues: Map<String, Any?> = emptyMap(),
    val report: List<ReportLine> = emptyList(),
    val notes: List<TestNote> = emptyList(),
    val error: String? = null,
    val failed: TestStep? = null,
    val done: TestStep? = null,
    val tookMs: Long? = null,
) {
    val view: TestView get() = trail.lastOrNull() ?: TestView.Hint

    // Opens a view. A list or the report starts the trail again. A view that is on the trail already becomes the last again, and another is added after the one shown.
    fun showing(next: TestView): ExtensionTest = copy(
        trail = when {
            next == TestView.List || next == TestView.Report || next == TestView.Hint -> listOf(next)
            next in trail -> trail.subList(0, trail.indexOf(next) + 1)
            else -> trail + next
        },
    )

    fun back(): ExtensionTest = copy(trail = trail.dropLast(1))
}
