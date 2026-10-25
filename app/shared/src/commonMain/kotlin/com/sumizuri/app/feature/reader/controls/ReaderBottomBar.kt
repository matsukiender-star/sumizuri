package com.sumizuri.app.feature.reader.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.reader.ReaderMode
import com.sumizuri.app.feature.reader.BarState
import com.sumizuri.app.ui.kit.dock.DockAction
import com.sumizuri.app.ui.kit.dock.DockMargin
import com.sumizuri.app.ui.kit.dock.DockRule
import com.sumizuri.app.ui.kit.dock.DockShape
import com.sumizuri.app.ui.kit.dock.PanelHead
import com.sumizuri.app.ui.kit.dock.dockEnter
import com.sumizuri.app.ui.kit.dock.dockExit
import com.sumizuri.app.ui.kit.dock.dockMorph
import com.sumizuri.app.ui.kit.dock.keepTaps
import com.sumizuri.app.ui.kit.dock.panelChange
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.manga_group_mode
import com.sumizuri.app.shared.resources.reader_auto
import com.sumizuri.app.shared.resources.reader_auto_stop
import com.sumizuri.app.shared.resources.reader_chapters
import com.sumizuri.app.shared.resources.reader_light
import com.sumizuri.app.shared.resources.reader_mode_button
import com.sumizuri.app.shared.resources.reader_mode_default
import com.sumizuri.app.shared.resources.reader_mode_make_default
import com.sumizuri.app.shared.resources.reader_mode_this_title
import com.sumizuri.app.shared.resources.reader_next_chapter
import com.sumizuri.app.shared.resources.reader_options
import com.sumizuri.app.shared.resources.reader_previous_chapter
import com.sumizuri.app.ui.theme.LocalReduceMotion
import org.jetbrains.compose.resources.stringResource

// The bottom of the reader as one floating dock that touches no edge of the screen, so the page shows all around it. Its upper row is the track between the chapter before and after with the page and the count at its ends, a hairline parts it from the row of actions under it, and the light, the settings, the chapters, the actions of a page and the meaning of a word open inside the dock above the track, which grows to hold them, so the reader needs no sheets.
@Composable
fun ReaderBottomBar(
    page: Int,
    total: Int,
    slider: Boolean,
    bar: BarState,
    previousId: Long?,
    nextId: Long?,
    // The numbers of the chapter before and after, shown beside their arrows.
    previousLabel: String?,
    nextLabel: String?,
    // Whether the row of small page pictures shows over the track.
    strip: Boolean,
    // How solid the dock is, from see through to fully solid.
    shade: Float,
    // The widest the dock grows on a big window.
    maxWidth: Dp,
    preview: (Int) -> PagePreview?,
    onJump: (Int) -> Unit,
    onOpenChapter: (Long) -> Unit,
    onMode: (ReaderMode) -> Unit,
    onLight: (dim: Float, warmth: Float) -> Unit,
    onLightDone: () -> Unit,
    onAuto: () -> Unit,
    // The panel the dock holds open over its track, the most room it may take, and what the settings, the chapters, a page or a word show in it.
    panel: DockPanel,
    onPanel: (DockPanel) -> Unit,
    panelMax: Dp,
    panelContent: @Composable (DockPanel) -> Unit,
    // Whether the whole dock is out, and what it shrinks to while the controls are hidden: a small pill with the page, or nothing when that is null.
    expanded: Boolean,
    collapsed: String?,
    onExpand: () -> Unit,
    // The mode of titles without a mode of their own, and a way to change it from the panel of modes.
    defaultMode: ReaderMode,
    onDefaultMode: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun toggle(which: DockPanel) = onPanel(if (panel == which) DockPanel.None else which)
    val reduce = LocalReduceMotion.current
    val colors = MaterialTheme.colorScheme
    // With the track down the side, the chapter buttons and the page go with it, and only the actions stay in the dock.
    val side = !slider && total > 1
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(DockMargin),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // The dock is one surface that changes its shape: the full dock while the controls are out, a small pill with the page while they rest, and gone when no pill is kept.
        AnimatedVisibility(expanded || collapsed != null, enter = dockEnter(fromTop = false, reduce), exit = dockExit(fromTop = false, reduce)) {
        Surface(
            Modifier.widthIn(max = maxWidth).keepTaps(),
            shape = DockShape,
            // Solid while a panel is open, so its words never sit over the page.
            color = colors.surfaceContainerHigh.copy(alpha = if (panel == DockPanel.None) shade else 1f),
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
        ) {
          AnimatedContent(expanded, transitionSpec = { dockMorph(reduce) }, label = "dock shape") { open ->
            if (!open) {
                Text(
                    collapsed.orEmpty(),
                    Modifier.clickable(onClickLabel = collapsed, onClick = onExpand).padding(horizontal = 14.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                )
                return@AnimatedContent
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                // The open panel grows out of the dock above the track, and a new one fades in where the last one was.
                AnimatedContent(
                    panel,
                    transitionSpec = { panelChange(reduce, DockPanel.None) },
                    label = "dock panel",
                ) { shown ->
                    if (shown != DockPanel.None) {
                        Column {
                            Box(Modifier.heightIn(max = panelMax)) {
                                when (shown) {
                                    DockPanel.Light -> LightPanel(bar.dim, bar.warmth, onLight, onLightDone)

                                    DockPanel.Mode -> ModePanel(bar, defaultMode, onDefaultMode, onClose = { onPanel(DockPanel.None) }) {
                                        onPanel(DockPanel.None)
                                        onMode(it)
                                    }

                                    else -> panelContent(shown)
                                }
                            }
                            DockRule()
                        }
                    }
                }
                if (strip && !side && total > 1 && preview(page) != null) {
                    PageStrip(page, total, preview, onJump, Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
                }
                if (!side) {
                    Row(Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                        ChapterStep(
                            Icons.Outlined.SkipPrevious,
                            stringResource(Res.string.reader_previous_chapter),
                            previousLabel,
                            previousId,
                            true,
                            onOpenChapter,
                        )
                        Text(
                            (page + 1).toString(),
                            Modifier.padding(start = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                        )
                        if (total > 1) {
                            PageTrack(
                                page,
                                total,
                                vertical = false,
                                preview,
                                onJump,
                                Modifier.weight(1f).padding(horizontal = 6.dp),
                            )
                        } else {
                            Box(Modifier.weight(1f))
                        }
                        Text(
                            total.toString(),
                            Modifier.padding(end = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant,
                        )
                        ChapterStep(
                            Icons.Outlined.SkipNext,
                            stringResource(Res.string.reader_next_chapter),
                            nextLabel,
                            nextId,
                            false,
                            onOpenChapter,
                        )
                    }
                    DockRule()
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    DockAction(
                        Icons.AutoMirrored.Outlined.FormatListBulleted,
                        stringResource(Res.string.reader_chapters),
                        panel == DockPanel.Chapters,
                        { toggle(DockPanel.Chapters) },
                    )
                    // The button shows the picture of the mode in use, and the modes open in the dock like every other panel.
                    DockAction(bar.mode.icon(), stringResource(Res.string.reader_mode_button), panel == DockPanel.Mode, {
                        toggle(DockPanel.Mode)
                    })
                    DockAction(
                        Icons.Outlined.LightMode,
                        stringResource(Res.string.reader_light),
                        panel == DockPanel.Light,
                        { toggle(DockPanel.Light) },
                    )
                    if (bar.canAuto) {
                        DockAction(
                            if (bar.auto) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                            stringResource(if (bar.auto) Res.string.reader_auto_stop else Res.string.reader_auto),
                            bar.auto,
                            onAuto,
                        )
                    }
                    DockAction(
                        Icons.Outlined.Tune,
                        stringResource(Res.string.reader_options),
                        panel == DockPanel.Settings,
                        { toggle(DockPanel.Settings) },
                    )
                }
            }
          }
        }
        }
    }
}

// The arrow to the chapter before or after with its number beside it, faded when there is none.
@Composable
private fun ChapterStep(
    icon: ImageVector,
    label: String,
    number: String?,
    target: Long?,
    iconFirst: Boolean,
    onOpenChapter: (Long) -> Unit,
) {
    val enabled = target != null
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.3f,
        )
    }
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) { target?.let(onOpenChapter) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconFirst) Icon(icon, label, Modifier.size(20.dp), tint = tint)
        if (number !=
            null
        ) {
            Text(
                number,
                Modifier.padding(horizontal = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        }
        if (!iconFirst) Icon(icon, label, Modifier.size(20.dp), tint = tint)
    }
}

// The ways to read as a panel of the dock. A mode picked here is kept for this title, which the line under the head says. The mode every other title opens in carries a small tag, and a button makes the mode of this title that default too, so the mode has one place and not two.
@Composable
private fun ModePanel(bar: BarState, defaultMode: ReaderMode, onDefaultMode: (ReaderMode) -> Unit, onClose: () -> Unit, onMode: (ReaderMode) -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Words read in pages under every mode but the scroll, so a mode stands for the row it falls under.
    fun row(mode: ReaderMode) = if (bar.textual && mode != ReaderMode.Continuous) ReaderMode.LeftToRight else mode
    Column {
        PanelHead(stringResource(Res.string.manga_group_mode), onClose)
        Text(
            stringResource(Res.string.reader_mode_this_title),
            Modifier.padding(start = 10.dp, bottom = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        val modes = if (bar.textual) listOf(ReaderMode.LeftToRight, ReaderMode.Continuous) else ReaderMode.entries
        for (mode in modes) {
            val chosen = mode == row(bar.mode)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (chosen) colors.secondaryContainer else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onMode(mode) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(mode.icon(), null, tint = if (chosen) colors.onSecondaryContainer else colors.onSurfaceVariant)
                Text(
                    stringResource(mode.labelFor(bar.textual)),
                    Modifier.weight(1f),
                    color = if (chosen) colors.onSecondaryContainer else colors.onSurface,
                    fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (mode == row(defaultMode)) {
                    Text(
                        stringResource(Res.string.reader_mode_default),
                        Modifier.background(colors.surfaceContainerHighest, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (chosen) Icon(Icons.Outlined.Check, null, tint = colors.primary)
            }
        }
        if (row(bar.mode) != row(defaultMode)) {
            TextButton({ onDefaultMode(bar.mode) }, Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.reader_mode_make_default))
            }
        }
    }
}
