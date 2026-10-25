package com.sumizuri.app.data.model.controls

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.chapter_action_bookmark
import com.sumizuri.app.shared.resources.chapter_action_select
import com.sumizuri.app.shared.resources.chapter_action_toggle_read
import com.sumizuri.app.shared.resources.gesture_none
import com.sumizuri.app.shared.resources.gesture_open
import com.sumizuri.app.shared.resources.nav_double_tap_global
import com.sumizuri.app.shared.resources.nav_double_tap_search
import com.sumizuri.app.shared.resources.nav_double_tap_top
import org.jetbrains.compose.resources.StringResource

// What a tap, a long press or a double tap on a cover of the library can do.
enum class LibraryCoverAction(val label: StringResource) {
    Open(Res.string.gesture_open),
    Select(Res.string.chapter_action_select),
    None(Res.string.gesture_none),
}

// What a swipe or a long press on a chapter row can do.
enum class ChapterRowAction(val label: StringResource) {
    ToggleRead(Res.string.chapter_action_toggle_read),
    ToggleBookmark(Res.string.chapter_action_bookmark),
    Select(Res.string.chapter_action_select),
    None(Res.string.gesture_none),
}

// What a second tap on the tab that is already open can do.
enum class NavDoubleTapAction(val label: StringResource) {
    None(Res.string.gesture_none),
    ScrollToTop(Res.string.nav_double_tap_top),
    Search(Res.string.nav_double_tap_search),

    // Opens the search of every installed source, for a title that is not in the library yet.
    GlobalSearch(Res.string.nav_double_tap_global),
}
