package com.sumizuri.app.data.model.controls

import com.sumizuri.app.data.model.controls.ReaderAction.AutoScrollFaster
import com.sumizuri.app.data.model.controls.ReaderAction.AutoScrollSlower
import com.sumizuri.app.data.model.controls.ReaderAction.Close
import com.sumizuri.app.data.model.controls.ReaderAction.NextChapter
import com.sumizuri.app.data.model.controls.ReaderAction.NextPage
import com.sumizuri.app.data.model.controls.ReaderAction.OpenChapters
import com.sumizuri.app.data.model.controls.ReaderAction.OpenInBrowser
import com.sumizuri.app.data.model.controls.ReaderAction.OpenSettings
import com.sumizuri.app.data.model.controls.ReaderAction.PageLeft
import com.sumizuri.app.data.model.controls.ReaderAction.PageRight
import com.sumizuri.app.data.model.controls.ReaderAction.PreviousChapter
import com.sumizuri.app.data.model.controls.ReaderAction.PreviousPage
import com.sumizuri.app.data.model.controls.ReaderAction.ScrollDown
import com.sumizuri.app.data.model.controls.ReaderAction.ScrollUp
import com.sumizuri.app.data.model.controls.ReaderAction.ToggleAutoScroll
import com.sumizuri.app.data.model.controls.ReaderAction.ToggleBookmark
import com.sumizuri.app.data.model.controls.ReaderAction.ToggleControls
import com.sumizuri.app.ui.controls.KeyChord

// How many cells the tap grid has, three across and three down.
const val ZoneCount = 9

// The keys, the tap grids and the scroll steps of the reader. Each action has its own keys and no key does two things.
data class ReaderControls(
    val keys: Map<ReaderAction, List<KeyChord>> = DefaultKeys,
    val tapZones: Map<TapLayout, List<ReaderAction?>> = DefaultZones,
    val scroll: ScrollSteps = ScrollSteps(),
    val geometry: Map<TapLayout, TapGeometry> = emptyMap(),
    // The volume keys of a phone turn the pages, the down key forward unless it is turned around.
    val volumeKeys: Boolean = false,
    val volumeInverted: Boolean = false,
    // Whether holding a finger on a picture page opens the menu of the page.
    val longTapMenu: Boolean = true,
) {
    fun withLongTapMenu(on: Boolean): ReaderControls = copy(longTapMenu = on)

    fun withVolumeKeys(on: Boolean): ReaderControls = copy(volumeKeys = on)

    fun withVolumeInverted(on: Boolean): ReaderControls = copy(volumeInverted = on)

    fun geometryOf(layout: TapLayout): TapGeometry = geometry[layout] ?: TapGeometry()

    fun actionForKey(pressed: KeyChord): ReaderAction? = keys.entries.firstOrNull { pressed in it.value }?.key

    // The action of the cell under a tap, given as how far across and down it landed from 0 to 1.
    fun actionForTap(layout: TapLayout, x: Float, y: Float): ReaderAction? {
        val sizes = geometryOf(layout)
        fun band(at: Float, edge: Float) = if (at < edge) {
            0
        } else if (at > 1 - edge) {
            2
        } else {
            1
        }
        return tapZones.getValue(layout)[band(y, sizes.edgeY) * 3 + band(x, sizes.edgeX)]
    }

    // The chord goes to the action, and is taken from any other action that had it.
    fun withKey(action: ReaderAction, chord: KeyChord): ReaderControls = copy(keys = keys.mapValues { (own, chords) -> if (own == action) chords.filter { it != chord } + chord else chords.filter { it != chord } })

    fun withoutKey(action: ReaderAction, chord: KeyChord): ReaderControls = copy(keys = keys.mapValues { (own, chords) -> if (own == action) chords.filter { it != chord } else chords })

    // The keys of one action back to the usual, leaving out any that another action holds now.
    fun withKeysReset(action: ReaderAction): ReaderControls {
        val taken = keys
            .filterKeys { it != action }
            .values
            .flatten()
            .toSet()
        return copy(keys = keys + (action to DefaultKeys.getValue(action).filter { it !in taken }))
    }

    fun withAllKeysReset(): ReaderControls = copy(keys = DefaultKeys)

    fun withTapZone(layout: TapLayout, zone: Int, action: ReaderAction?): ReaderControls = copy(tapZones = tapZones + (layout to tapZones.getValue(layout).toMutableList().also { it[zone] = action }))

    // The cells of the grid and the sizes of its bands back to the usual.
    fun withTapLayoutReset(layout: TapLayout): ReaderControls = copy(tapZones = tapZones + (layout to DefaultZones.getValue(layout)), geometry = geometry - layout)

    // The left and the right of the grid swapped, for the other hand.
    fun withTapMirrored(layout: TapLayout): ReaderControls {
        val zones = tapZones.getValue(layout)
        return copy(tapZones = tapZones + (layout to List(ZoneCount) { zones[it / 3 * 3 + (2 - it % 3)] }))
    }

    fun withGeometry(layout: TapLayout, value: TapGeometry): ReaderControls = copy(geometry = geometry + (layout to value))

    fun withPreset(layout: TapLayout, preset: TapPreset): ReaderControls {
        val (zones, sizes) = presetOf(layout, preset)
        return copy(tapZones = tapZones + (layout to zones), geometry = geometry + (layout to sizes))
    }

    fun withScroll(steps: ScrollSteps): ReaderControls = copy(scroll = steps)

    val isDefault: Boolean get() = this == ReaderControls()

    companion object {
        val DefaultKeys: Map<ReaderAction, List<KeyChord>> = mapOf(
            NextPage to listOf(KeyChord("Space"), KeyChord("PageDown")),
            PreviousPage to listOf(KeyChord("PageUp")),
            PageRight to listOf(KeyChord("Right")),
            PageLeft to listOf(KeyChord("Left")),
            ScrollDown to listOf(KeyChord("Down")),
            ScrollUp to listOf(KeyChord("Up")),
            ToggleControls to listOf(KeyChord("Escape")),
            NextChapter to emptyList(),
            PreviousChapter to emptyList(),
            OpenSettings to emptyList(),
            OpenChapters to emptyList(),
            OpenInBrowser to emptyList(),
            ToggleBookmark to emptyList(),
            ReaderAction.PageMenu to emptyList(),
            ToggleAutoScroll to emptyList(),
            AutoScrollFaster to emptyList(),
            AutoScrollSlower to emptyList(),
            Close to emptyList(),
        )

        val DefaultZones: Map<TapLayout, List<ReaderAction?>> = mapOf(
            TapLayout.Paged to listOf(PageLeft, PreviousPage, PageRight, PageLeft, ToggleControls, PageRight, PageLeft, NextPage, PageRight),
            TapLayout.Continuous to listOf(PreviousPage, PreviousPage, PreviousPage, ToggleControls, ToggleControls, ToggleControls, NextPage, NextPage, NextPage),
        )
    }
}
