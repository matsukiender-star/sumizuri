package com.sumizuri.app.feature.blockscript

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.blocks_finished
import com.sumizuri.app.shared.resources.blocks_recent
import com.sumizuri.app.shared.resources.choice_figures
import com.sumizuri.app.shared.resources.choice_year
import com.sumizuri.app.shared.resources.library_continue
import com.sumizuri.app.shared.resources.library_title
import com.sumizuri.app.shared.resources.profile_chapters_read
import com.sumizuri.app.shared.resources.profile_stats
import com.sumizuri.app.shared.resources.simple_added
import com.sumizuri.app.shared.resources.simple_day
import com.sumizuri.app.shared.resources.simple_days_read
import com.sumizuri.app.shared.resources.simple_favorites
import com.sumizuri.app.shared.resources.simple_formula
import com.sumizuri.app.shared.resources.simple_genre
import com.sumizuri.app.shared.resources.simple_hour
import com.sumizuri.app.shared.resources.simple_last_7
import com.sumizuri.app.shared.resources.simple_month
import com.sumizuri.app.shared.resources.simple_most_unread
import com.sumizuri.app.shared.resources.simple_reading
import com.sumizuri.app.shared.resources.simple_section
import com.sumizuri.app.shared.resources.simple_shape_circle
import com.sumizuri.app.shared.resources.simple_shape_round
import com.sumizuri.app.shared.resources.simple_shape_square
import com.sumizuri.app.shared.resources.simple_style_area
import com.sumizuri.app.shared.resources.simple_style_bars
import com.sumizuri.app.shared.resources.simple_style_columns
import com.sumizuri.app.shared.resources.simple_style_donut
import com.sumizuri.app.shared.resources.simple_style_dots
import com.sumizuri.app.shared.resources.simple_style_line
import com.sumizuri.app.shared.resources.simple_style_pie
import com.sumizuri.app.shared.resources.simple_style_radial
import com.sumizuri.app.shared.resources.simple_style_share
import com.sumizuri.app.shared.resources.simple_text_body
import com.sumizuri.app.shared.resources.simple_text_caption
import com.sumizuri.app.shared.resources.simple_text_title
import com.sumizuri.app.shared.resources.simple_time
import com.sumizuri.app.shared.resources.simple_unread
import com.sumizuri.app.shared.resources.simple_weekday
import com.sumizuri.app.shared.resources.sort_author
import com.sumizuri.app.shared.resources.sort_kind
import com.sumizuri.app.shared.resources.sort_rating
import com.sumizuri.app.shared.resources.sort_source
import com.sumizuri.app.shared.resources.sort_status
import com.sumizuri.app.shared.resources.sort_title
import com.sumizuri.app.shared.resources.stats_current_streak
import com.sumizuri.app.shared.resources.stats_last_month
import com.sumizuri.app.shared.resources.stats_longest_streak
import com.sumizuri.app.shared.resources.stats_per_week
import com.sumizuri.app.shared.resources.stats_today
import com.sumizuri.app.shared.resources.view_kind_bar
import com.sumizuri.app.shared.resources.view_kind_chart
import com.sumizuri.app.shared.resources.view_kind_covers
import com.sumizuri.app.shared.resources.view_kind_heatmap
import com.sumizuri.app.shared.resources.view_kind_list
import com.sumizuri.app.shared.resources.view_kind_number
import com.sumizuri.app.shared.resources.view_kind_ring
import com.sumizuri.app.shared.resources.view_kind_text
import org.jetbrains.compose.resources.StringResource

// The names the simple editor gives its choices, by the keys the simple block keeps.
internal val ShowNames: List<Pair<String, StringResource>> = listOf(
    "number" to Res.string.view_kind_number,
    "ring" to Res.string.view_kind_ring,
    "bar" to Res.string.view_kind_bar,
    "chart" to Res.string.view_kind_chart,
    "list" to Res.string.view_kind_list,
    "covers" to Res.string.view_kind_covers,
    "heatmap" to Res.string.view_kind_heatmap,
    "text" to Res.string.view_kind_text,
    "section" to Res.string.simple_section,
)

internal val ValueNames: Map<String, StringResource> = mapOf(
    "stats.streak" to Res.string.stats_current_streak,
    "stats.read" to Res.string.profile_chapters_read,
    "stats.today" to Res.string.stats_today,
    "stats.week" to Res.string.simple_last_7,
    "stats.month" to Res.string.stats_last_month,
    "stats.best" to Res.string.stats_longest_streak,
    "stats.days" to Res.string.simple_days_read,
    "stats.pace" to Res.string.stats_per_week,
    "stats.library" to Res.string.library_title,
    "stats.favorites" to Res.string.simple_favorites,
    "stats.unread" to Res.string.simple_unread,
    "finished" to Res.string.blocks_finished,
    "reading" to Res.string.simple_reading,
    "formula" to Res.string.simple_formula,
)

internal val KeyNames: Map<String, StringResource> = mapOf(
    "genre" to Res.string.simple_genre,
    "kind" to Res.string.sort_kind,
    "status" to Res.string.sort_status,
    "source" to Res.string.sort_source,
    "author" to Res.string.sort_author,
    "weekday" to Res.string.simple_weekday,
    "day" to Res.string.simple_day,
    "time" to Res.string.simple_time,
    "hour" to Res.string.simple_hour,
    "month" to Res.string.simple_month,
    "title" to Res.string.sort_title,
)

internal val StyleNames: List<Pair<String, StringResource>> = listOf(
    "bars" to Res.string.simple_style_bars,
    "columns" to Res.string.simple_style_columns,
    "line" to Res.string.simple_style_line,
    "area" to Res.string.simple_style_area,
    "dots" to Res.string.simple_style_dots,
    "donut" to Res.string.simple_style_donut,
    "pie" to Res.string.simple_style_pie,
    "radial" to Res.string.simple_style_radial,
    "share" to Res.string.simple_style_share,
)

internal val TitlesNames: Map<String, StringResource> = mapOf(
    "recent" to Res.string.blocks_recent,
    "added" to Res.string.simple_added,
    "unread" to Res.string.simple_most_unread,
    "favorites" to Res.string.simple_favorites,
    "rating" to Res.string.sort_rating,
)

internal val ShapeNames = listOf(
    "round" to Res.string.simple_shape_round,
    "square" to Res.string.simple_shape_square,
    "circle" to Res.string.simple_shape_circle,
)

internal val TextStyleNames = listOf(
    "title" to Res.string.simple_text_title,
    "body" to Res.string.simple_text_body,
    "caption" to Res.string.simple_text_caption,
)

internal val SectionNames = listOf(
    "figures" to Res.string.choice_figures,
    "year" to Res.string.choice_year,
    "continue" to Res.string.library_continue,
    "statistics" to Res.string.profile_stats,
)

// The colours offered as dots, after the colour of the theme.
internal val ColorNames =
    listOf("red", "orange", "amber", "yellow", "lime", "green", "teal", "cyan", "blue", "indigo", "purple", "pink")
