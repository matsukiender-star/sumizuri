package com.sumizuri.app.feature.profile.blocks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.data.blocks.BlockView
import com.sumizuri.app.data.blocks.ChartKind
import com.sumizuri.app.data.blocks.ClassicSection
import com.sumizuri.app.feature.profile.ShownBlock
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.choice_figures
import com.sumizuri.app.shared.resources.choice_year
import com.sumizuri.app.shared.resources.library_continue
import com.sumizuri.app.shared.resources.profile_stats
import com.sumizuri.app.shared.resources.view_kind_bar
import com.sumizuri.app.shared.resources.view_kind_chart
import com.sumizuri.app.shared.resources.view_kind_covers
import com.sumizuri.app.shared.resources.view_kind_empty
import com.sumizuri.app.shared.resources.view_kind_heatmap
import com.sumizuri.app.shared.resources.view_kind_list
import com.sumizuri.app.shared.resources.view_kind_number
import com.sumizuri.app.shared.resources.view_kind_problem
import com.sumizuri.app.shared.resources.view_kind_ring
import com.sumizuri.app.shared.resources.view_kind_stack
import com.sumizuri.app.shared.resources.view_kind_text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// What kind of view a block shows, in words.
private fun BlockView.kind(): StringResource = when (this) {
    is BlockView.Figure -> Res.string.view_kind_number

    is BlockView.Ring -> Res.string.view_kind_ring

    is BlockView.Bar -> Res.string.view_kind_bar

    is BlockView.Chart -> Res.string.view_kind_chart

    is BlockView.Items -> Res.string.view_kind_list

    is BlockView.Covers -> Res.string.view_kind_covers

    is BlockView.Heatmap -> Res.string.view_kind_heatmap

    is BlockView.Words -> Res.string.view_kind_text

    is BlockView.Stack -> Res.string.view_kind_stack

    is BlockView.Classic -> when (section) {
        ClassicSection.Figures -> Res.string.choice_figures
        ClassicSection.Year -> Res.string.choice_year
        ClassicSection.Continue -> Res.string.library_continue
        ClassicSection.Statistics -> Res.string.profile_stats
    }

    is BlockView.Problem -> Res.string.view_kind_problem

    BlockView.Empty -> Res.string.view_kind_empty
}

// The first label a view has, looking into a stack, which names the block better than its kind does.
private fun BlockView.label(): String? = when (this) {
    is BlockView.Figure -> label
    is BlockView.Ring -> label
    is BlockView.Bar -> label
    is BlockView.Chart -> label
    is BlockView.Items -> label
    is BlockView.Covers -> label
    is BlockView.Heatmap -> label
    is BlockView.Words -> text.lineSequence().firstOrNull()
    is BlockView.Stack -> parts.firstNotNullOfOrNull { it.label()?.takeIf(String::isNotBlank) }
    else -> null
}?.takeIf { it.isNotBlank() }

// A name for a block in the list of blocks: the title of its script, else the label of its view, else what kind of view it is.
@Composable
internal fun ShownBlock.name(): String = title.takeIf { it.isNotBlank() } ?: view.label() ?: stringResource(view.kind())

// What kind of view a block shows, as a word under its name.
@Composable
internal fun ShownBlock.kindName(): String = stringResource(view.kind())

// An icon for what a block shows.
internal fun BlockView.icon(): ImageVector = when (this) {
    is BlockView.Figure -> Icons.Outlined.Tag

    is BlockView.Ring -> Icons.Outlined.DonutLarge

    is BlockView.Bar -> Icons.Outlined.LinearScale

    is BlockView.Chart -> when (style.kind) {
        ChartKind.Line, ChartKind.Area -> Icons.AutoMirrored.Outlined.ShowChart
        ChartKind.Donut, ChartKind.Pie, ChartKind.Radial, ChartKind.Share -> Icons.Outlined.PieChart
        else -> Icons.Outlined.BarChart
    }

    is BlockView.Items -> Icons.AutoMirrored.Outlined.ViewList

    is BlockView.Covers -> Icons.Outlined.PhotoLibrary

    is BlockView.Heatmap -> Icons.Outlined.GridOn

    is BlockView.Words -> Icons.Outlined.TextFields

    is BlockView.Stack -> Icons.Outlined.ViewAgenda

    is BlockView.Classic -> Icons.Outlined.Dashboard

    is BlockView.Problem -> Icons.Outlined.ErrorOutline

    BlockView.Empty -> Icons.Outlined.CropFree
}
