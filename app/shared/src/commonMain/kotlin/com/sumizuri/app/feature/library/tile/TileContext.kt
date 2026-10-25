package com.sumizuri.app.feature.library.tile

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import com.sumizuri.app.data.model.library.CoverBadgeLayout
import com.sumizuri.app.data.model.library.SourceBrief

// What a tile needs to know besides the title: where the marks sit, whether the progress bar shows, and the names of the sources, since a title only holds the id of its source.
class TileContext(
    val badges: CoverBadgeLayout,
    val showProgress: Boolean,
    val favoritesEnabled: Boolean,
    val sources: Map<String, SourceBrief>,
    // How many times wider than high a tile of the wide grid is, how wide the cover of a list row is, and how big the names are.
    val panoramaAspect: Float = 16f / 9f,
    val listCover: Int = 52,
    val nameScale: Float = 1f,
)

// What a tap, a long press and a double tap on a tile do. A double tap that does nothing is null, so a single tap is not held back to wait for it.
class TileCalls(
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
    val onDoubleClick: (() -> Unit)?,
    // Goes on reading the title from where it was left, offered on a cover the pointer rests on.
    val onContinue: (() -> Unit)? = null,
)

// The style with its size and its line height made bigger or smaller by the part given, so names follow the size the person chose.
internal fun TextStyle.scaled(by: Float): TextStyle = if (by ==
    1f
) {
    this
} else {
    copy(fontSize = fontSize * by, lineHeight = if (lineHeight.isSpecified) lineHeight * by else lineHeight)
}
