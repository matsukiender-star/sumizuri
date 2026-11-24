package com.sumizuri.app.feature.browse.source

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.feature.browse.asTitle
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_preview_loading
import com.sumizuri.app.shared.resources.suki_preview_no_summary
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.text.displayName
import org.jetbrains.compose.resources.stringResource

// Delay before showing the hover panel or asking the source for missing details.
internal const val HoverPreviewDelayMs = 320L

// Whether a list entry already carries enough metadata to skip a details request.
internal fun SourceEntry.hasPreviewInfo(): Boolean = !description.isNullOrBlank() || genres.isNotEmpty()

// Merges fetched details with the list entry so cover and title are never lost.
internal fun SourceEntry.mergeDetails(details: SourceEntry?): SourceEntry {
    if (details == null) return this
    return copy(
        title = details.title.ifBlank { title },
        coverUrl = details.coverUrl ?: coverUrl,
        author = details.author?.takeIf { it.isNotBlank() } ?: author,
        description = details.description?.takeIf { it.isNotBlank() } ?: description,
        status = details.status?.takeIf { it.isNotBlank() } ?: status,
        genres = details.genres.ifEmpty { genres },
    )
}

// Tracks mouse enter/exit only, leaving touch gestures untouched.
internal fun Modifier.mouseHoverPreview(
    enabled: Boolean,
    key: Any,
    onEnter: () -> Unit,
    onExit: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(key) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.none { it.type == PointerType.Mouse }) continue
                when (event.type) {
                    PointerEventType.Enter -> onEnter()
                    PointerEventType.Exit -> onExit()
                    else -> Unit
                }
            }
        }
    }
}

// Floating preview card anchored at the bottom while hovering an item with the mouse on desktop.
@Composable
internal fun HoverPreviewBanner(
    entry: SourceEntry?,
    kind: Kind,
    sourceId: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = entry != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val shown = entry ?: return@AnimatedVisibility
        val title = shown.asTitle(kind, sourceId)
        val colors = MaterialTheme.colorScheme
        val shape = MaterialTheme.shapes.large
        // Use a plain Box instead of Material Surface so pointer hover/clicks pass through to the items underneath.
        Box(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(colors.surfaceContainerHigh, shape)
                .border(1.dp, colors.outlineVariant, shape),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.width(72.dp)) {
                        CoverArt(title, Modifier.fillMaxWidth(), letterSize = 20)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            title.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        PreviewMetaLine(title.author, title.status)
                        PreviewGenres(title.genres, maxGenres = 6)
                        PreviewSummary(title.summary, loading, maxLines = 4)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMetaLine(author: String, status: Status) {
    val statusText = if (status == Status.Unknown) null else stringResource(status.label)
    val facts = listOfNotNull(author.takeIf { it.isNotBlank() }, statusText)
    if (facts.isNotEmpty()) {
        Text(
            facts.joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewGenres(genres: List<String>, maxGenres: Int) {
    if (genres.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (genre in genres.take(maxGenres)) {
            Box(Modifier.background(colors.secondaryContainer, MaterialTheme.shapes.small)) {
                Text(
                    genre,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PreviewSummary(summary: String, loading: Boolean, maxLines: Int) {
    val colors = MaterialTheme.colorScheme
    when {
        summary.isNotBlank() -> Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )

        loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InkCircularProgressIndicator(Modifier.size(16.dp))
            Text(
                stringResource(Res.string.suki_preview_loading),
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline,
            )
        }

        else -> Text(
            stringResource(Res.string.suki_preview_no_summary),
            style = MaterialTheme.typography.bodySmall,
            color = colors.outline,
        )
    }
}
