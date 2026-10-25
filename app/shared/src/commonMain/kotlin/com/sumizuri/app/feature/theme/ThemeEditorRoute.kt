package com.sumizuri.app.feature.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_copy_suffix
import com.sumizuri.app.shared.resources.editor_discard
import com.sumizuri.app.shared.resources.editor_discard_message
import com.sumizuri.app.shared.resources.editor_discard_title
import com.sumizuri.app.shared.resources.editor_keep_editing
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemePresets
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// A window at least this wide has the settings in a column beside the preview, and a narrower one has them under it.
private val SideBySideWidth = 900.dp

// How wide the column of settings is beside the preview, and wider in a large window so every tab name fits.
private val ControlsWidth = 440.dp
private val WideControlsWidth = 480.dp
private val LargeWindowWidth = 1300.dp

// The preview can be laid out as in a desktop window when it has at least this much room, and is when the settings sit beside it, until the person picks.
private val DesktopPreviewWidth = 640.dp

// How much of the height the preview takes over the settings on a narrow screen at first, and how far the handle between them can move it.
private const val StartSplit = 0.42f
private val SplitRange = 0.2f..0.75f

// The editor of a theme: the app drawn in the theme, the tabs of settings always in reach beside or under it, and a bar with undo, redo, the mode of the preview, a hold to see the theme as it was and Save. A built in theme is saved as a copy of its own.
@Composable
fun ThemeEditorRoute(themeId: String, onBack: () -> Unit) {
    val themes = rememberThemeModel()
    val mine by themes.custom.collectAsStateWithLifecycle()
    val found = (ThemePresets + mine).firstOrNull { it.id == themeId }
    if (found == null) {
        // A theme that is not there, or not read yet, gets a moment before the editor gives up.
        LaunchedEffect(themeId) {
            delay(800)
            onBack()
        }
        return
    }
    val startDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val state = remember(found.id) { ThemeEditorState(found, startDark) }
    val copyName = stringResource(Res.string.editor_copy_suffix, state.draft.name)
    var leaving by remember { mutableStateOf(false) }
    fun save() {
        val draft = state.draft
        themes.save(
            if (draft.isBuiltIn) {
                draft.copy(
                    id = AppThemeSpec.CustomPrefix + newThemeId(),
                    name = if (draft.name ==
                        state.original.name
                    ) {
                        copyName
                    } else {
                        draft.name
                    },
                )
            } else {
                draft
            },
        )
        onBack()
    }
    fun close() {
        if (state.changed) leaving = true else onBack()
    }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = state.changed) {
        leaving =
            true
    }
    val keys = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keys.requestFocus() } }
    Box(
        Modifier.fillMaxSize().focusRequester(keys).focusable().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val command = event.isCtrlPressed || event.isMetaPressed
            when {
                command && event.key == Key.Z && event.isShiftPressed -> state.redo()
                command && event.key == Key.Z -> state.undo()
                command && event.key == Key.Y -> state.redo()
                command && event.key == Key.S -> save()
                event.key == Key.Escape -> close()
                else -> return@onPreviewKeyEvent false
            }
            true
        },
    ) {
        EditorLayout(state, onClose = ::close, onSave = ::save)
    }
    if (leaving) {
        AppDialog(
            onDismissRequest = { leaving = false },
            title = { Text(stringResource(Res.string.editor_discard_title)) },
            text = { Text(stringResource(Res.string.editor_discard_message)) },
            confirmButton = { InkButton(onBack) { Text(stringResource(Res.string.editor_discard)) } },
            dismissButton = {
                InkOutlinedButton(
                    { leaving = false },
                ) { Text(stringResource(Res.string.editor_keep_editing)) }
            },
        )
    }
}

@Composable
private fun EditorLayout(state: ThemeEditorState, onClose: () -> Unit, onSave: () -> Unit) {
    var split by remember { mutableFloatStateOf(StartSplit) }
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        val wide = maxWidth >= SideBySideWidth
        val controlsWidth = if (maxWidth >= LargeWindowWidth) WideControlsWidth else ControlsWidth
        val desktopFits = (if (wide) maxWidth - controlsWidth else maxWidth) >= DesktopPreviewWidth
        val desktop = desktopFits && (state.desktop ?: wide)
        val height = maxHeight
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        Column(Modifier.fillMaxSize()) {
            EditorBar(state, desktop = desktop.takeIf { desktopFits }, onClose, onSave)
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    PreviewPane(state, desktop, Modifier.weight(1f).fillMaxHeight())
                    EditorControls(state, Modifier.width(controlsWidth).fillMaxHeight())
                }
            } else {
                PreviewPane(state, desktop, Modifier.fillMaxWidth().height(height * split))
                // A handle between the preview and the settings, dragged to give either more room.
                Box(
                    Modifier.fillMaxWidth().height(18.dp).draggable(
                        rememberDraggableState { delta ->
                            split =
                                (split + delta / heightPx).coerceIn(SplitRange.start, SplitRange.endInclusive)
                        },
                        Orientation.Vertical,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(
                                width = 40.dp,
                                height = 4.dp,
                            ).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                EditorControls(state, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

// The preview in a frame like a screen that fills all the room it has, scrolled on its own so the settings never cover it. A desktop page takes the whole width, and a phone stands in the middle at the width of a phone.
@Composable
private fun PreviewPane(state: ThemeEditorState, desktop: Boolean, modifier: Modifier) {
    val selection = EditorSelection(state.focus, state.reveal, state::select)
    val frame = RoundedCornerShape(20.dp)
    val titles = rememberPreviewTitles()
    CompositionLocalProvider(LocalEditorSelection provides selection) {
        BoxWithConstraints(modifier) {
            val height = maxHeight - 24.dp
            Box(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                ThemePreview(
                    if (state.comparing) state.original else state.draft,
                    state.dark,
                    desktop,
                    (
                        if (desktop) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .widthIn(
                                    max = 420.dp,
                                ).fillMaxWidth()
                        }
                        ).shadow(8.dp, frame).clip(frame),
                    height,
                    titles,
                )
            }
        }
    }
}
