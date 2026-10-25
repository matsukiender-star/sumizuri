package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.data.blocks.BlockView
import com.sumizuri.app.data.blocks.ClassicSection
import com.sumizuri.app.data.blocks.ItemRow
import com.sumizuri.app.data.blocks.WordStyle
import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.data.model.stats.ReadingStats
import com.sumizuri.app.feature.library.part.TitleStrip
import com.sumizuri.app.feature.profile.ProfileFigures
import com.sumizuri.app.feature.profile.ProfileStatistics
import com.sumizuri.app.feature.profile.YearStrip
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.blocks_empty
import com.sumizuri.app.shared.resources.library_continue
import com.sumizuri.app.shared.resources.profile_stats
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll
import com.sumizuri.app.ui.text.displayName
import org.jetbrains.compose.resources.stringResource

// What a block needs from the page besides its own view: the statistics and the titles being read for the parts of the profile as it was, how wide the page is, and a way to open a title.
class BlockEnv(val stats: ReadingStats, val recent: List<FeedItem>, val wide: Boolean, val onOpenTitle: (Long) -> Unit)

// Draws what a block shows. A view in a block with a set height fills it, and one that fits its content takes the height it needs.
@Composable
fun BlockViewContent(view: BlockView, env: BlockEnv, modifier: Modifier = Modifier) {
    when (view) {
        is BlockView.Figure -> FigureView(view, modifier)

        is BlockView.Ring -> RingView(view, modifier)

        is BlockView.Bar -> BarView(view, modifier)

        is BlockView.Chart -> ChartView(view, modifier)

        is BlockView.Heatmap -> HeatmapView(view, modifier)

        is BlockView.Items -> ItemsView(view, env, modifier)

        is BlockView.Covers -> CoversView(view, env, modifier)

        is BlockView.Words -> WordsView(view, modifier)

        is BlockView.Stack -> StackView(view, env, modifier)

        is BlockView.Classic -> ClassicView(view.section, env, modifier)

        is BlockView.Problem -> ProblemView(view, modifier)

        BlockView.Empty -> Text(
            stringResource(Res.string.blocks_empty),
            modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// A label over a view, which is left out when it is blank.
@Composable
internal fun ViewLabel(label: String) {
    if (label.isNotBlank()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FigureView(view: BlockView.Figure, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center) {
        ViewLabel(view.label)
        val size = when {
            view.value.length > 12 -> 18.sp
            view.value.length > 6 -> 24.sp
            else -> 32.sp
        }
        Text(
            view.value,
            style = MaterialTheme.typography.headlineMedium,
            fontSize = size,
            fontWeight = FontWeight.ExtraBold,
            color = viewColor(view.color, MaterialTheme.colorScheme.onSurface),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (view.caption.isNotBlank()) {
            Text(
                view.caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RingView(view: BlockView.Ring, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val ink = viewColor(view.color)
    BoxWithConstraints(modifier) {
        // A narrow block puts the label under the ring, and a wide one beside it.
        val narrow = maxWidth < 180.dp
        val ring = @Composable {
            Box(Modifier.size(if (narrow) 72.dp else 88.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.11f
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    drawArc(
                        ink,
                        -90f,
                        360f * view.fraction,
                        false,
                        Offset(inset, inset),
                        arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text(
                    view.center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        if (narrow) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ring()
                ViewLabel(view.label)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ring()
                ViewLabel(view.label)
            }
        }
    }
}

@Composable
private fun BarView(view: BlockView.Bar, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ViewLabel(view.label) }
            Text(view.value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(view.fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(viewColor(view.color)),
            )
        }
    }
}

@Composable
private fun WordsView(view: BlockView.Words, modifier: Modifier) {
    when (view.style) {
        WordStyle.Title -> Text(
            view.text,
            modifier,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        WordStyle.Body -> Text(view.text, modifier, style = MaterialTheme.typography.bodyMedium)

        WordStyle.Caption -> Text(
            view.text,
            modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun StackView(view: BlockView.Stack, env: BlockEnv, modifier: Modifier) {
    if (view.row) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (part in view.parts) BlockViewContent(part, env, Modifier.weight(1f))
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (part in view.parts) BlockViewContent(part, env, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ItemsView(view: BlockView.Items, env: BlockEnv, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ViewLabel(view.label)
        for (row in view.rows) ItemLine(row, env)
    }
}

@Composable
private fun ItemLine(row: ItemRow, env: BlockEnv) {
    val title = row.title
    Row(
        Modifier.fillMaxWidth().then(if (title != null) Modifier.clickable { env.onOpenTitle(title.id) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) CoverArt(title, Modifier.width(26.dp), letterSize = 10)
        Text(
            row.name?.let {
                stringResource(it)
            } ?: title?.displayName() ?: row.label,
            Modifier.weight(
                1f,
            ),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (row.value.isNotEmpty()) {
            Text(
                row.value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CoversView(view: BlockView.Covers, env: BlockEnv, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ViewLabel(view.label)
        val row = rememberLazyListState()
        LazyRow(
            Modifier.mouseDragScroll(row, horizontal = true),
            state = row,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(view.titles, key = { it.id }) { title ->
                Column(Modifier.width(84.dp).clickable { env.onOpenTitle(title.id) }) {
                    CoverArt(title, Modifier.fillMaxWidth(), letterSize = 20)
                    Text(
                        title.displayName(),
                        Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassicView(section: ClassicSection, env: BlockEnv, modifier: Modifier) {
    Box(modifier) {
        when (section) {
            ClassicSection.Figures -> ProfileFigures(env.stats)

            ClassicSection.Year -> YearStrip(env.stats)

            ClassicSection.Continue -> TitleStrip(Res.string.library_continue, env.recent, env.onOpenTitle)

            ClassicSection.Statistics -> Column {
                AppSectionLabel(stringResource(Res.string.profile_stats))
                ProfileStatistics(env.stats, env.wide)
            }
        }
    }
}

@Composable
internal fun ProblemView(problem: BlockView.Problem, modifier: Modifier = Modifier) {
    val message = stringResource(problem.text, *problem.args.toTypedArray())
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
        Text(
            if (problem.at.isEmpty()) message else "${problem.at}: $message",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
