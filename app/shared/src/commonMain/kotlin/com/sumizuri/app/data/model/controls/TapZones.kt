package com.sumizuri.app.data.model.controls

import com.sumizuri.app.data.model.controls.ReaderAction.NextPage
import com.sumizuri.app.data.model.controls.ReaderAction.PageLeft
import com.sumizuri.app.data.model.controls.ReaderAction.PageRight
import com.sumizuri.app.data.model.controls.ReaderAction.PreviousPage
import com.sumizuri.app.data.model.controls.ReaderAction.ToggleControls
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.tap_layout_continuous
import com.sumizuri.app.shared.resources.tap_layout_paged
import com.sumizuri.app.shared.resources.tap_preset_edges
import com.sumizuri.app.shared.resources.tap_preset_l_shaped
import com.sumizuri.app.shared.resources.tap_preset_menu_only
import com.sumizuri.app.shared.resources.tap_preset_standard
import com.sumizuri.app.shared.resources.tap_preset_wide_center
import org.jetbrains.compose.resources.StringResource

// How wide the bands on the sides of the tap grid are and how tall the top and the bottom ones are, as shares of the screen.
data class TapGeometry(val edgeX: Float = 0.3f, val edgeY: Float = 0.3f) {
    companion object {
        const val Smallest = 0.15f
        const val Largest = 0.45f
    }
}

// The two ways the reader lays a chapter out, which have a tap grid each since a tap does different things on a page and on a long scroll.
enum class TapLayout(val label: StringResource) {
    Paged(Res.string.tap_layout_paged),
    Continuous(Res.string.tap_layout_continuous),
}

// Ready made tap layouts, so a good arrangement is one tap away.
enum class TapPreset(val label: StringResource) {
    // The sides turn the page and the middle shows the controls.
    Standard(Res.string.tap_preset_standard),

    // Only the strips on the left and the right turn the page.
    Edges(Res.string.tap_preset_edges),

    // Back on the left and the top, forward on the right and the bottom.
    LShaped(Res.string.tap_preset_l_shaped),

    // A big middle, so a stray tap rarely turns the page.
    WideCenter(Res.string.tap_preset_wide_center),

    // Nothing turns the page, and only the middle shows the controls.
    MenuOnly(Res.string.tap_preset_menu_only),
}

// The cells and the band sizes a preset gives to a layout.
internal fun presetOf(layout: TapLayout, preset: TapPreset): Pair<List<ReaderAction?>, TapGeometry> {
    val paged = layout == TapLayout.Paged
    val p = PreviousPage
    val n = NextPage
    val o = ToggleControls
    val zones: List<ReaderAction?> = when (preset) {
        TapPreset.Standard, TapPreset.WideCenter -> ReaderControls.DefaultZones.getValue(layout)
        TapPreset.Edges -> if (paged) listOf(PageLeft, o, PageRight, PageLeft, o, PageRight, PageLeft, o, PageRight) else listOf(p, o, p, null, o, null, n, o, n)
        TapPreset.LShaped -> listOf(p, p, p, p, o, n, n, n, n)
        TapPreset.MenuOnly -> listOf(null, null, null, null, o, null, null, null, null)
    }
    val sizes = if (preset == TapPreset.WideCenter) TapGeometry(0.2f, 0.2f) else TapGeometry()
    return zones to sizes
}
