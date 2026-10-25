package com.sumizuri.app.feature.reader.lookup

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle

// What to do with a word that was held down. It is null where words cannot be looked up.
val LocalWordLookup = staticCompositionLocalOf<((String) -> Unit)?> { null }

// Text of a novel. Holding a finger or the pointer on a word asks for its meaning. A plain tap goes on to the reader as before, and the tap that ends a hold is taken here so it does not also toggle the bars.
@Composable
fun LookupText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val lookup = LocalWordLookup.current
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val press = if (lookup == null) {
        modifier
    } else {
        modifier.pointerInput(text, lookup) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                val word = layout?.let { WordPick.at(text, it.getOffsetForPosition(held.position)) }
                if (word != null) {
                    lookup(word)
                    waitForUpOrCancellation()?.consume()
                }
            }
        }
    }
    Text(text, press, style = style, onTextLayout = { layout = it })
}
