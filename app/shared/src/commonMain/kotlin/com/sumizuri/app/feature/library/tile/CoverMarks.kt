package com.sumizuri.app.feature.library.tile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.SourceState
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.CoverBadgeKind
import com.sumizuri.app.data.model.library.CoverCorner
import com.sumizuri.app.ui.kit.cover.badge.SourceWarningBadge
import com.sumizuri.app.ui.kit.cover.badge.UnreadBadge
import com.sumizuri.app.ui.kit.cover.sourceName
import org.jetbrains.compose.resources.stringResource

// The marks on a cover, each in the corner the person chose for it. A title whose source is gone or off always has its warning at the bottom left, since that matters more than the look.
@Composable
internal fun BoxScope.CoverMarks(title: Title, context: TileContext, picking: Boolean) {
    // The bar of progress runs along the bottom edge, so the marks at the bottom sit above it.
    val lift = if (context.showProgress) 3.dp else 0.dp
    for (corner in CoverCorner.entries) {
        if (corner == CoverCorner.Off) continue
        // The ring for picking sits in the top left corner, and the marks that would be there wait.
        if (picking && corner == CoverCorner.TopStart) continue
        val kinds = context.badges.at(corner).filter { shows(it, title, context) }
        val warning = corner == CoverCorner.BottomStart && title.sourceState != SourceState.Ready
        if (kinds.isEmpty() && !warning) continue
        Row(
            Modifier.align(corner.alignment()).padding(
                start = 6.dp,
                end = 6.dp,
                top = 6.dp,
                bottom = if (corner ==
                    CoverCorner.BottomStart ||
                    corner == CoverCorner.BottomEnd
                ) {
                    6.dp + lift
                } else {
                    6.dp
                },
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (warning) SourceWarningBadge()
            for (kind in kinds) Mark(kind, title, context)
        }
    }
}

private fun CoverCorner.alignment(): Alignment = when (this) {
    CoverCorner.TopStart -> Alignment.TopStart
    CoverCorner.TopEnd -> Alignment.TopEnd
    CoverCorner.BottomStart -> Alignment.BottomStart
    CoverCorner.BottomEnd -> Alignment.BottomEnd
    CoverCorner.Off -> Alignment.TopStart
}

private fun shows(kind: CoverBadgeKind, title: Title, context: TileContext): Boolean = when (kind) {
    CoverBadgeKind.Unread -> title.unread > 0
    CoverBadgeKind.Status -> title.status != com.sumizuri.app.data.model.Status.Unknown
    CoverBadgeKind.Source -> context.sources[title.sourceId] != null
    CoverBadgeKind.Language -> context.sources[title.sourceId]?.lang?.isNotBlank() == true
    CoverBadgeKind.Favorite -> title.favorite && context.favoritesEnabled
}

@Composable
private fun Mark(kind: CoverBadgeKind, title: Title, context: TileContext) {
    when (kind) {
        CoverBadgeKind.Unread -> UnreadBadge(title.unread)

        CoverBadgeKind.Status -> TextMark(stringResource(title.status.label))

        CoverBadgeKind.Source -> TextMark(
            context.sources[title.sourceId]
                ?.let {
                    sourceName(title.sourceId, it.name)
                }.orEmpty(),
        )

        CoverBadgeKind.Language -> TextMark(
            context.sources[title.sourceId]
                ?.lang
                .orEmpty()
                .uppercase(),
        )

        CoverBadgeKind.Favorite -> Box(
            Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiary)
        }
    }
}

// A short word on a small dark chip, readable on any cover.
@Composable
private fun TextMark(text: String) {
    Text(
        text,
        Modifier
            .widthIn(max = 84.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
