package com.sumizuri.app.feature.title.chapter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.sumizuri.app.feature.title.ChapterCallbacks
import com.sumizuri.app.feature.title.ChapterItem
import com.sumizuri.app.ui.controls.LocalAppGestures

// A tap opens the chapter, or picks it while others are picked, and a long press or a right click does what the person chose for it. A preview opens its chapters but has nothing to pick yet.
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.chapterClicks(
    chapter: ChapterItem,
    calls: ChapterCallbacks,
    selecting: Boolean,
    preview: Boolean,
): Modifier {
    val gestures = LocalAppGestures.current
    // A mouse has no long press people think of, so its right button does the same.
    val rightClick = if (preview) {
        Modifier
    } else {
        Modifier.pointerInput(chapter, calls, gestures) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        gestures.chapterLongPress.perform(chapter, calls)
                    }
                }
            }
        }
    }
    return then(rightClick).combinedClickable(
        onLongClick = if (preview) {
            null
        } else {
            { gestures.chapterLongPress.perform(chapter, calls) }
        },
        onClick = { if (selecting) calls.onSelect(chapter.id) else calls.onOpen(chapter.id) },
    )
}

// The mark on a chapter while chapters are being picked: a tick when it is picked and an empty ring when not.
@Composable
internal fun PickMark(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Icon(
        if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
        null,
        modifier,
        tint = if (selected) colors.primary else colors.outline,
    )
}
