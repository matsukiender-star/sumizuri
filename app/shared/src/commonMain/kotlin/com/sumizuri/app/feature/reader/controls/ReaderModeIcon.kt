package com.sumizuri.app.feature.reader.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.East
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.West
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.data.model.reader.ReaderMode
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.reader_novel_paged
import com.sumizuri.app.shared.resources.reader_novel_scroll
import org.jetbrains.compose.resources.StringResource

// The picture on the button of each reading mode.
fun ReaderMode.icon(): ImageVector = when (this) {
    ReaderMode.Continuous -> Icons.Outlined.ViewDay
    ReaderMode.RightToLeft -> Icons.Outlined.West
    ReaderMode.LeftToRight -> Icons.Outlined.East
    ReaderMode.Vertical -> Icons.Outlined.SwapVert
}

// The name of a mode. For words only two exist, scrolled and in pages, and every mode that is not the scroll reads as pages.
fun ReaderMode.labelFor(textual: Boolean): StringResource = when {
    !textual -> label
    this == ReaderMode.Continuous -> Res.string.reader_novel_scroll
    else -> Res.string.reader_novel_paged
}
