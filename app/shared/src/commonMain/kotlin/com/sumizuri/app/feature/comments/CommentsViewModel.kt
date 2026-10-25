package com.sumizuri.app.feature.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.SourceError
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.browser.ChallengeSolver
import com.sumizuri.app.source.model.CommentPage
import com.sumizuri.app.source.model.CommentSort
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceComment
import com.sumizuri.app.source.model.SourceEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// What the comments are about: a title of a source, or one of its chapters when the chapter is given. The names are for the bar of the page.
data class CommentsTarget(
    val sourceId: String,
    val url: String,
    val name: String,
    val chapterUrl: String? = null,
    val chapterName: String? = null,
)

// The replies of one comment that were asked for: the ones there are, the last page asked, and whether there are no more, one is being asked or asking failed.
data class RepliesState(
    val replies: List<SourceComment> = emptyList(),
    val page: Int = 0,
    val ended: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

// The comments as they came, in the order asked for, a page at a time, or why they could not be read. Missing is true when the source is not installed or is off.
data class CommentsState(
    val loading: Boolean = true,
    val sort: CommentSort = CommentSort.Newest,
    val comments: List<SourceComment> = emptyList(),
    // How many there are in all, when the source says.
    val total: Int? = null,
    // The last page read, whether there are no more, whether the next is being read, and why it could not be.
    val page: Int = 0,
    val ended: Boolean = false,
    val loadingMore: Boolean = false,
    val moreError: String? = null,
    val error: String? = null,
    val challengeUrl: String? = null,
    val missing: Boolean = false,
    // Whether the source gives the replies of a comment, and the replies asked for so far by the key of their comment.
    val canLoadReplies: Boolean = false,
    val replies: Map<String, RepliesState> = emptyMap(),
)

// What tells one comment from another, when a page comes back with some of the ones before it.
internal fun SourceComment.key(): String = id ?: "$author|$postedAt|${text.hashCode()}"

// Reads the comments of a title or a chapter from its source a page at a time, again from the start when the order changes or the person asks, and the replies of a comment when asked.
class CommentsViewModel(
    private val target: CommentsTarget,
    private val sources: SourceRepository,
    private val manager: SourceManager,
    private val solver: ChallengeSolver,
) : ViewModel() {
    private val mutable = MutableStateFlow(CommentsState())
    val state: StateFlow<CommentsState> = mutable
    private var job: Job? = null
    private var source: Source? = null

    init {
        load()
    }

    fun onSort(sort: CommentSort) {
        if (sort == mutable.value.sort && !mutable.value.loading) return
        mutable.update { it.copy(sort = sort) }
        load()
    }

    fun onRefresh() = load()

    // Opens the site for a person to pass the bot check, then asks again.
    fun onSolve() {
        val url = mutable.value.challengeUrl ?: return
        viewModelScope.launch {
            solver.solve(url)
            load()
        }
    }

    // The next page, when the end of the list comes into view.
    fun onMore() {
        val now = mutable.value
        if (now.loading || now.loadingMore || now.ended || now.error != null) return
        val opened = source ?: return
        val next = now.page + 1
        mutable.update { it.copy(loadingMore = true, moreError = null) }
        job = viewModelScope.launch {
            when (val result = fetch(opened, now.sort, next)) {
                is SourceResult.Ok -> mutable.update { it.withPage(result.value, next) }
                is SourceResult.Err -> mutable.update { it.copy(loadingMore = false, moreError = result.error.message) }
            }
        }
    }

    // The next page of replies to a comment, the first one when none were asked yet.
    fun onReplies(comment: SourceComment) {
        val id = comment.id ?: return
        val opened = source ?: return
        val was = mutable.value.replies[id] ?: RepliesState(replies = comment.replies)
        if (was.loading || was.ended) return
        val next = was.page + 1
        mutable.update { it.copy(replies = it.replies + (id to was.copy(loading = true, error = null))) }
        viewModelScope.launch {
            val result = opened.commentReplies(id, next)
            mutable.update { now ->
                val current = now.replies[id] ?: was
                val updated = when (result) {
                    is SourceResult.Ok -> {
                        val known = current.replies.mapTo(HashSet()) { it.key() }
                        val fresh = result.value.comments
                            .filter { it.key() !in known }
                            .distinctBy { it.key() }
                        val all = current.replies + fresh
                        current.copy(replies = all, page = next, loading = false, ended = ended(result.value, fresh.size, all.size, comment.replyCount.takeIf { it > 0 }))
                    }

                    is SourceResult.Err -> current.copy(loading = false, error = result.error.message)
                }
                now.copy(replies = now.replies + (id to updated))
            }
        }
    }

    private fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null, challengeUrl = null, missing = false, comments = emptyList(), page = 0, ended = false, loadingMore = false, moreError = null, total = null, replies = emptyMap()) }
            val installed = sources.find(target.sourceId)?.takeIf { it.enabled }
            if (installed == null) {
                mutable.update { it.copy(loading = false, missing = true) }
                return@launch
            }
            val opened = when (val result = manager.open(installed)) {
                is SourceResult.Err -> return@launch fail(result.error)
                is SourceResult.Ok -> result.value
            }
            source = opened
            mutable.update { it.copy(canLoadReplies = opened.info.hasCommentReplies) }
            when (val result = fetch(opened, mutable.value.sort, 1)) {
                is SourceResult.Ok -> mutable.update { it.copy(loading = false).withPage(result.value, 1) }
                is SourceResult.Err -> fail(result.error)
            }
        }
    }

    private fun fail(error: SourceError) {
        mutable.update { it.copy(loading = false, error = error.message, challengeUrl = (error as? SourceError.Challenge)?.url) }
    }

    private suspend fun fetch(opened: Source, sort: CommentSort, page: Int): SourceResult<CommentPage> {
        val chapterUrl = target.chapterUrl
        return if (chapterUrl != null) {
            opened.chapterComments(SourceChapter(url = chapterUrl, title = target.chapterName.orEmpty()), sort, page)
        } else {
            opened.comments(SourceEntry(url = target.url, title = target.name), sort, page)
        }
    }

    // A page added to the list, less the comments it already has.
    private fun CommentsState.withPage(page: CommentPage, number: Int): CommentsState {
        val known = comments.mapTo(HashSet()) { it.key() }
        val fresh = page.comments.filter { it.key() !in known }.distinctBy { it.key() }
        val all = comments + fresh
        val total = page.total ?: total
        return copy(comments = all, page = number, total = total, loadingMore = false, ended = ended(page, fresh.size, all.size, total))
    }
}

// Whether a list has come to its end: the source says so, every one there is was read, or a page brought nothing new, as a source that ignores the page gives the first one again.
private fun ended(page: CommentPage, fresh: Int, have: Int, total: Int?): Boolean = when {
    page.hasNextPage == false -> true
    total != null && have >= total -> true
    else -> fresh == 0
}
