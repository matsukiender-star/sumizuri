package com.sumizuri.app.feature.blockscript

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.guide_chart_options
import com.sumizuri.app.shared.resources.guide_chart_view
import com.sumizuri.app.shared.resources.guide_colors
import com.sumizuri.app.shared.resources.guide_condition
import com.sumizuri.app.shared.resources.guide_count
import com.sumizuri.app.shared.resources.guide_count_by
import com.sumizuri.app.shared.resources.guide_data
import com.sumizuri.app.shared.resources.guide_fields
import com.sumizuri.app.shared.resources.guide_filter
import com.sumizuri.app.shared.resources.guide_if_view
import com.sumizuri.app.shared.resources.guide_label
import com.sumizuri.app.shared.resources.guide_library
import com.sumizuri.app.shared.resources.guide_list_views
import com.sumizuri.app.shared.resources.guide_lists
import com.sumizuri.app.shared.resources.guide_math
import com.sumizuri.app.shared.resources.guide_math_functions
import com.sumizuri.app.shared.resources.guide_number_view
import com.sumizuri.app.shared.resources.guide_operators
import com.sumizuri.app.shared.resources.guide_reads
import com.sumizuri.app.shared.resources.guide_ring_view
import com.sumizuri.app.shared.resources.guide_shape
import com.sumizuri.app.shared.resources.guide_sort
import com.sumizuri.app.shared.resources.guide_stack_view
import com.sumizuri.app.shared.resources.guide_stats
import com.sumizuri.app.shared.resources.guide_stats_more
import com.sumizuri.app.shared.resources.guide_sum_of
import com.sumizuri.app.shared.resources.guide_text_functions
import com.sumizuri.app.shared.resources.guide_text_view
import com.sumizuri.app.shared.resources.guide_titles_of
import com.sumizuri.app.shared.resources.guide_top
import com.sumizuri.app.shared.resources.guide_views
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// A piece of script and what it does. The piece goes into the script with a tap.
private class GuideLine(val code: String, val about: StringResource)

private class GuidePart(val title: StringResource, val lines: List<GuideLine>)

private val Parts = listOf(
    GuidePart(
        Res.string.guide_shape,
        listOf(
            GuideLine(
                "{\n  \"title\": \"\",\n  \"let\": { \"goal\": 60 },\n  \"view\": { \"ring\": \"stats.month\", \"max\": \"goal\" }\n}",
                Res.string.guide_label,
            ),
        ),
    ),
    GuidePart(
        Res.string.guide_data,
        listOf(
            GuideLine("stats.read  stats.today  stats.week  stats.month", Res.string.guide_stats),
            GuideLine(
                "stats.streak  stats.best  stats.days  stats.library  stats.favorites  stats.unread  stats.pace",
                Res.string.guide_stats_more,
            ),
            GuideLine("library", Res.string.guide_library),
            GuideLine("reads(30)", Res.string.guide_reads),
        ),
    ),
    GuidePart(
        Res.string.guide_lists,
        listOf(
            GuideLine("filter(library, unread > 0)", Res.string.guide_filter),
            GuideLine("count(library, favorite)", Res.string.guide_count),
            GuideLine("countBy(library, 'genre')", Res.string.guide_count_by),
            GuideLine("top(countBy(reads, 'title'), 5)", Res.string.guide_top),
            GuideLine("sort(library, 'unread')", Res.string.guide_sort),
            GuideLine("sumOf(library, unread)", Res.string.guide_sum_of),
            GuideLine("titlesOf(reads(7))", Res.string.guide_titles_of),
            GuideLine(
                "name author kind status genres source unread favorite rating chapters added day daysAgo hour weekday label value",
                Res.string.guide_fields,
            ),
        ),
    ),
    GuidePart(
        Res.string.guide_math,
        listOf(
            GuideLine("+ - * / % ^  == != < <= > >=  && || !  a ? b : c", Res.string.guide_operators),
            GuideLine("round(x, 1) min max abs clamp percent(a, b) if(test, a, b)", Res.string.guide_math_functions),
            GuideLine("contains(genres, 'Action') upper lower len text(x, 1)", Res.string.guide_text_functions),
            GuideLine("status == 'Completed' && unread == 0", Res.string.guide_condition),
        ),
    ),
    GuidePart(
        Res.string.guide_views,
        listOf(
            GuideLine(
                "{ \"number\": \"stats.streak\", \"label\": \"\", \"caption\": \"\" }",
                Res.string.guide_number_view,
            ),
            GuideLine(
                "{ \"ring\": \"stats.month\", \"max\": 100, \"label\": \"{stats.month} / 100\" }",
                Res.string.guide_ring_view,
            ),
            GuideLine(
                "{ \"chart\": \"countBy(reads, 'weekday')\", \"style\": \"columns\" }",
                Res.string.guide_chart_view,
            ),
            GuideLine(
                "{ \"chart\": \"countBy(reads(30), 'day')\", \"style\": \"area\", \"color\": \"teal\", \"smooth\": true, \"height\": 140 }",
                Res.string.guide_chart_options,
            ),
            GuideLine(
                "{ \"heatmap\": \"reads(365)\", \"color\": \"#4CAF6E\", \"shape\": \"circle\" }",
                Res.string.guide_colors,
            ),
            GuideLine("{ \"covers\": \"take(titlesOf(reads(30)), 10)\" }", Res.string.guide_list_views),
            GuideLine(
                "{ \"text\": \"Read {stats.month} this month\", \"style\": \"title\" }",
                Res.string.guide_text_view,
            ),
            GuideLine("{ \"stack\": [ ] }", Res.string.guide_stack_view),
            GuideLine("{ \"if\": \"stats.streak > 0\", \"then\": { }, \"else\": { } }", Res.string.guide_if_view),
        ),
    ),
)

// The guide to scripts: their shape, the data, the functions on lists, the sums and the views, each with a piece to tap in.
@Composable
internal fun ScriptGuide(onInsert: (String) -> Unit) {
    val code = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (part in Parts) {
            AppSectionLabel(stringResource(part.title))
            for (line in part.lines) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                        .clickable { onInsert(line.code) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(line.code, style = code)
                    Text(
                        stringResource(line.about),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
