package com.sumizuri.app.ui.kit.cover.focus.face

import com.sumizuri.app.ui.kit.cover.focus.CoverFocus
import com.sumizuri.app.ui.kit.cover.focus.FaceLevel

// A face needs this many windows that agree, since one window alone is mostly a speckle of hair or letters. The tight level asks for more.
private const val MinAgree = 2
private const val MinAgreeTight = 3

// A face is not looked for lower than this on a cover, where the titles are. The tight level stops higher.
private const val LowestFace = 0.7f
private const val LowestFaceTight = 0.55f

// The face to centre on, which is the one the most windows agree on and the biggest, or null when there is no face to trust. The result is as fractions of the width and the height.
fun focusOfFaces(faces: List<FaceBox>, width: Int, height: Int, level: FaceLevel): CoverFocus? {
    val agree = if (level == FaceLevel.Tight) MinAgreeTight else MinAgree
    val lowest = if (level == FaceLevel.Tight) LowestFaceTight else LowestFace
    val best = faces
        .filter { it.neighbors >= agree && it.y / height < lowest }
        .maxByOrNull { it.neighbors * it.size } ?: return null
    return CoverFocus(best.x / width, best.y / height, width.toFloat() / height)
}
