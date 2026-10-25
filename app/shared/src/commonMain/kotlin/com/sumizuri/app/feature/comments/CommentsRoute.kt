package com.sumizuri.app.feature.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.comments_chapter_title
import com.sumizuri.app.shared.resources.comments_count
import com.sumizuri.app.shared.resources.comments_empty
import com.sumizuri.app.shared.resources.comments_hide_replies
import com.sumizuri.app.shared.resources.comments_load_more
import com.sumizuri.app.shared.resources.comments_missing_source
import com.sumizuri.app.shared.resources.comments_more_failed
import com.sumizuri.app.shared.resources.comments_more_replies
import com.sumizuri.app.shared.resources.comments_replies_failed
import com.sumizuri.app.shared.resources.comments_show_replies
import com.sumizuri.app.shared.resources.comments_sort_newest
import com.sumizuri.app.shared.resources.comments_sort_oldest
import com.sumizuri.app.shared.resources.comments_sort_top
import com.sumizuri.app.shared.resources.comments_spoiler
import com.sumizuri.app.shared.resources.comments_title
import com.sumizuri.app.shared.resources.refresh
import com.sumizuri.app.source.model.CommentSort
import com.sumizuri.app.source.model.SourceComment
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.chips.FilterTabs
import com.sumizuri.app.ui.kit.image.imageRequest
import com.sumizuri.app.ui.kit.page.LoadStatus
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.page.PageList
import com.sumizuri.app.ui.kit.page.PageTopBar
import com.sumizuri.app.ui.kit.page.ReadableColumn
import com.sumizuri.app.ui.text.agoText
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// How many replies a comment shows before the rest fold away behind a button.
private const val RepliesAtFirst = 3

@Composable
fun CommentsRoute(target: CommentsTarget, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "comments_${target.sourceId}_${target.url}_${target.chapterUrl.orEmpty()}") {
        CommentsViewModel(target, container.sources, container.manager, container.solver)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CommentsScreen(
        target,
        state,
        CommentCalls(
            onBack,
            viewModel::onSort,
            viewModel::onRefresh,
            viewModel::onSolve,
            viewModel::onMore,
            viewModel::onReplies,
        ),
    )
}

// What the page of comments can ask for.
private class CommentCalls(
    val onBack: () -> Unit,
    val onSort: (CommentSort) -> Unit,
    val onRefresh: () -> Unit,
    val onSolve: () -> Unit,
    val onMore: () -> Unit,
    val onReplies: (SourceComment) -> Unit,
)

// The comments of a title or a chapter as the site has them, with the order to read them in. More come as the end of the list comes into view, and each comment can open its replies.
@Composable
private fun CommentsScreen(target: CommentsTarget, state: CommentsState, calls: CommentCalls) {
    ReadableColumn {
        Column(Modifier.fillMaxSize()) {
            val names = listOfNotNull(target.name, target.chapterName).joinToString(" · ")
            val count = state.total?.let { pluralStringResource(Res.plurals.comments_count, it, it) }
            PageTopBar(
                stringResource(
                    if (target.chapterUrl !=
                        null
                    ) {
                        Res.string.comments_chapter_title
                    } else {
                        Res.string.comments_title
                    },
                ),
                subtitle = listOfNotNull(count, names).joinToString(" · "),
                onBack = calls.onBack,
            ) {
                IconButton(calls.onRefresh, enabled = !state.loading) {
                    Icon(Icons.Outlined.Refresh, stringResource(Res.string.refresh))
                }
            }
            FilterTabs(
                CommentSort.entries.map {
                    it to stringResource(sortLabel(it))
                },
                state.sort,
                calls.onSort,
                Modifier.padding(horizontal = PageGutter),
            )
            PageList(Modifier.fillMaxSize()) {
                when {
                    state.missing -> item { Quiet(stringResource(Res.string.comments_missing_source)) }

                    state.loading || state.error != null -> item {
                        LoadStatus(
                            state.loading,
                            state.error,
                            calls.onRefresh,
                            onSolve = if (state.challengeUrl !=
                                null
                            ) {
                                calls.onSolve
                            } else {
                                null
                            },
                        )
                    }

                    state.comments.isEmpty() -> item { Quiet(stringResource(Res.string.comments_empty)) }

                    else -> {
                        items(state.comments, key = { it.key() }) { comment ->
                            CommentCard(comment, state.replies[comment.id], state.canLoadReplies, calls.onReplies)
                        }
                        if (!state.ended) item(key = "more") { MoreFooter(state, calls.onMore) }
                    }
                }
            }
        }
    }
}

// The end of what is read so far. Coming into view asks for the next page, and when that fails a button asks again.
@Composable
private fun MoreFooter(state: CommentsState, onMore: () -> Unit) {
    LaunchedEffect(state.comments.size) { onMore() }
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        when {
            state.loadingMore -> InkCircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)

            state.moreError != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.comments_more_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onMore) { Text(stringResource(Res.string.comments_load_more)) }
            }

            else -> TextButton(onMore) { Text(stringResource(Res.string.comments_load_more)) }
        }
    }
}

// One comment with its replies under it. The replies the source gave come first, a few at a time, and the source is asked for the rest.
@Composable
private fun CommentCard(
    comment: SourceComment,
    asked: RepliesState?,
    canLoad: Boolean,
    onReplies: (SourceComment) -> Unit,
) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CommentBody(comment, avatar = 36.dp)
            Replies(comment, asked, canLoad, onReplies)
        }
    }
}

@Composable
private fun Replies(
    comment: SourceComment,
    asked: RepliesState?,
    canLoad: Boolean,
    onReplies: (SourceComment) -> Unit,
) {
    val all = asked?.replies ?: comment.replies
    var open by rememberSaveable(comment.key()) { mutableStateOf(false) }
    val shown = if (open) all else all.take(RepliesAtFirst)
    // The replies the source still has, beyond the ones there are here.
    val waiting = (comment.replyCount - all.size).coerceAtLeast(0)
    val canAsk = canLoad && comment.id != null && asked?.ended != true
    if (shown.isEmpty() && (waiting == 0 || !canAsk)) return
    Column(Modifier.padding(start = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (reply in shown) {
            CommentBody(reply, avatar = 26.dp)
            // A reply's own replies, as far as the source gave them.
            if (reply.replies.isNotEmpty()) {
                Column(
                    Modifier.padding(start = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (deeper in reply.replies) CommentBody(deeper, avatar = 22.dp)
                }
            }
        }
        val folded = all.size - shown.size
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                folded > 0 -> SmallButton(pluralStringResource(Res.plurals.comments_show_replies, folded, folded)) {
                    open =
                        true
                }

                waiting > 0 && canAsk && asked?.loading != true -> SmallButton(
                    pluralStringResource(Res.plurals.comments_more_replies, waiting, waiting),
                ) {
                    open = true
                    onReplies(comment)
                }

                open && all.size > RepliesAtFirst -> SmallButton(stringResource(Res.string.comments_hide_replies)) {
                    open =
                        false
                }
            }
            if (asked?.loading == true) InkCircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
        asked?.error?.let {
            Text(
                stringResource(Res.string.comments_replies_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
