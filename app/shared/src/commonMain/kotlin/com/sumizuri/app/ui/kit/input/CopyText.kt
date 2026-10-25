package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

// A clipboard entry that holds this text and nothing else, made in the way each system has.
expect fun plainTextClip(text: String): ClipEntry

// Gives a function that puts a text on the clipboard.
@Composable
fun rememberCopyText(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return { text -> scope.launch { clipboard.setClipEntry(plainTextClip(text)) } }
}
