package com.sumizuri.app.ui.kit.cover.focus

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.BiasAlignment
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.face_level_normal
import com.sumizuri.app.shared.resources.face_level_tight
import com.sumizuri.app.ui.kit.cover.focus.face.FaceCascade
import com.sumizuri.app.ui.kit.cover.focus.face.detect
import com.sumizuri.app.ui.kit.cover.focus.face.equalizedGray
import com.sumizuri.app.ui.kit.cover.focus.face.focusOfFaces
import org.jetbrains.compose.resources.StringResource
import kotlin.math.max

// Where the interesting part of a cover is, usually a face, as fractions of its width and height, and the width of the cover over its height.
data class CoverFocus(val x: Float, val y: Float, val aspect: Float)

// The alignment that puts the focus in the middle of a view the cover fills, as near as the edges of the cover allow. The sizes only need to be in the same unit.
fun focusAlignment(focus: CoverFocus?, viewWidth: Float, viewHeight: Float, fallbackY: Float = -0.4f): BiasAlignment {
    if (focus == null || viewWidth <= 0f || viewHeight <= 0f) return BiasAlignment(0f, fallbackY)
    // The drawn height of the cover once it covers the view.
    val drawnHeight = max(viewWidth / focus.aspect, viewHeight)
    val drawnWidth = drawnHeight * focus.aspect

    fun axis(at: Float, drawn: Float, shown: Float): Float {
        val spare = drawn - shown
        if (spare <= 0f) return 0f
        val offset = (at * drawn - shown / 2f).coerceIn(0f, spare)
        return offset / spare * 2f - 1f
    }
    return BiasAlignment(axis(focus.x, drawnWidth, viewWidth), axis(focus.y, drawnHeight, viewHeight))
}

// How strict the face finder is. Tight leaves out the zones at the top and the bottom of a cover where logos and titles sit, and trusts only faces that it is sure of.
enum class FaceLevel(val label: StringResource) {
    Normal(Res.string.face_level_normal),
    Tight(Res.string.face_level_tight),
}

// The level the person chose, for every cover the page looks at.
val LocalFaceLevel = compositionLocalOf { FaceLevel.Normal }

// The skin finder, the one the Flutter app uses, is tuned for a picture this wide.
private const val SkinWidth = 64

// Where to centre on a cover. A face the cascade is sure of wins, and without one the skin finder of the Flutter app decides.
suspend fun focusOfCover(cover: CoverPixels, level: FaceLevel): CoverFocus {
    val cascade = FaceCascade.get()
    val face = cascade?.let {
        focusOfFaces(it.detect(equalizedGray(cover), cover.width, cover.height), cover.width, cover.height, level)
    }
    return face ?: focusOfPixels(cover.scaledToWidth(SkinWidth), ignoreTitles = level == FaceLevel.Tight)
}
