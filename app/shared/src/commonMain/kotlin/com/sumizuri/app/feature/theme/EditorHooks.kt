package com.sumizuri.app.feature.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// The blue of the editor itself, which no theme can change, so what is selected never looks like part of the theme.
internal val EditorBlue = Color(0xFF3D8BFF)

// What the preview needs to know: the piece in focus, a count that goes up when the preview should scroll to it, and what to do when a piece is tapped. Without it the preview is only a picture.
internal class EditorSelection(val focus: EditorPart?, val reveal: Int, val onSelect: (EditorPart) -> Unit)

internal val LocalEditorSelection = compositionLocalOf<EditorSelection?> { null }

// Marks a piece of the preview as a part of the app. A tap on it opens its settings, the innermost piece being the one that answers, the piece in focus is outlined in the blue of the editor, and the preview scrolls to it when its tab is opened.
internal fun Modifier.editTarget(part: EditorPart): Modifier = composed {
    val editor = LocalEditorSelection.current ?: return@composed this
    val requester = remember { BringIntoViewRequester() }
    val focused = editor.focus == part
    LaunchedEffect(editor.reveal) { if (focused && editor.reveal > 0) requester.bringIntoView() }
    val outlined = if (focused) Modifier.border(2.dp, EditorBlue) else Modifier
    bringIntoViewRequester(requester).then(outlined).pointerInput(part) { detectTapGestures { editor.onSelect(part) } }
}
