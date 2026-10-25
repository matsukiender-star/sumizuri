package com.sumizuri.app.data.blocks

import com.sumizuri.app.data.model.Title
import org.jetbrains.compose.resources.StringResource

// What a block shows, as its script describes it. The profile page draws it. A colour is a name of the theme, such as primary or tertiary, a plain name such as teal, or #RRGGBB, and null takes the main colour of the theme.
sealed interface BlockView {
    // A big number or word with a label over it and a line under it.
    data class Figure(val value: String, val label: String, val caption: String, val color: String? = null) : BlockView

    // A ring filled to a share of its whole, with the share or the value in the middle.
    data class Ring(val fraction: Float, val center: String, val label: String, val color: String? = null) : BlockView

    // A bar filled to a share of its whole.
    data class Bar(val fraction: Float, val value: String, val label: String, val color: String? = null) : BlockView

    data class Chart(val points: List<Point>, val label: String, val style: ChartStyle = ChartStyle()) : BlockView

    // Lines of a label and a value, with the cover of the title when a line is one.
    data class Items(val rows: List<ItemRow>, val label: String) : BlockView

    data class Covers(val titles: List<Title>, val label: String) : BlockView

    // The days of the last weeks as cells, a column for each week, darker for more chapters. Days are numbered from 1970 on the clock of the device.
    data class Heatmap(
        val daily: Map<Long, Int>,
        val today: Long,
        val weeks: Int,
        val label: String,
        val color: String? = null,
        val shape: CellShape = CellShape.Round,
    ) : BlockView

    data class Words(val text: String, val style: WordStyle) : BlockView

    // Views one under the other, or side by side.
    data class Stack(val parts: List<BlockView>, val row: Boolean) : BlockView

    // A part of the profile as it was before blocks, drawn by the page itself.
    data class Classic(val section: ClassicSection) : BlockView

    // What went wrong, shown in place of the view: words of the app, the pieces of the script they name, and where in the script it was.
    data class Problem(val text: StringResource, val args: List<Any> = emptyList(), val at: String = "") : BlockView

    // The script shows nothing yet.
    data object Empty : BlockView
}

// The kinds of chart: bars across, columns up, a line, a line with the space under it filled, a ring cut into slices, a pie, rings inside each other, dots on lines, and one bar split into shares.
enum class ChartKind { Bars, Columns, Line, Area, Donut, Pie, Radial, Dots, Share }

// How a chart is drawn. Colors go to the points in turn, and with none the chart takes its color, or the main colour of the theme. Values and labels can be left out, the height of a chart that goes up is in dp, max sets the top of the scale in place of the biggest value, and a smooth line curves through its points.
data class ChartStyle(
    val kind: ChartKind = ChartKind.Bars,
    val color: String? = null,
    val colors: List<String> = emptyList(),
    val values: Boolean? = null,
    val labels: Boolean = true,
    val height: Int? = null,
    val max: Double? = null,
    val smooth: Boolean = false,
)

enum class CellShape { Square, Round, Circle }

enum class WordStyle { Title, Body, Caption }

enum class ClassicSection { Figures, Year, Continue, Statistics }

class ItemRow(val label: String, val value: String, val title: Title? = null, val name: StringResource? = null)
