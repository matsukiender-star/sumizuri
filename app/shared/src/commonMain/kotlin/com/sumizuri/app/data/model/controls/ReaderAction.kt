package com.sumizuri.app.data.model.controls

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.reader_action_auto_faster
import com.sumizuri.app.shared.resources.reader_action_auto_slower
import com.sumizuri.app.shared.resources.reader_action_bookmark
import com.sumizuri.app.shared.resources.reader_action_chapters
import com.sumizuri.app.shared.resources.reader_action_close
import com.sumizuri.app.shared.resources.reader_action_controls
import com.sumizuri.app.shared.resources.reader_action_next_chapter
import com.sumizuri.app.shared.resources.reader_action_next_page
import com.sumizuri.app.shared.resources.reader_action_options
import com.sumizuri.app.shared.resources.reader_action_page_left
import com.sumizuri.app.shared.resources.reader_action_page_menu
import com.sumizuri.app.shared.resources.reader_action_page_right
import com.sumizuri.app.shared.resources.reader_action_previous_chapter
import com.sumizuri.app.shared.resources.reader_action_previous_page
import com.sumizuri.app.shared.resources.reader_action_scroll_down
import com.sumizuri.app.shared.resources.reader_action_scroll_up
import com.sumizuri.app.shared.resources.reader_action_toggle_auto
import com.sumizuri.app.shared.resources.reader_action_web
import org.jetbrains.compose.resources.StringResource

// Everything the reader can be told to do from a key or a tap. The page left and page right turn the way the book reads, and the next and previous page turn forward and back.
enum class ReaderAction(val label: StringResource) {
    NextPage(Res.string.reader_action_next_page),
    PreviousPage(Res.string.reader_action_previous_page),
    PageRight(Res.string.reader_action_page_right),
    PageLeft(Res.string.reader_action_page_left),
    ScrollDown(Res.string.reader_action_scroll_down),
    ScrollUp(Res.string.reader_action_scroll_up),
    ToggleControls(Res.string.reader_action_controls),
    NextChapter(Res.string.reader_action_next_chapter),
    PreviousChapter(Res.string.reader_action_previous_chapter),
    OpenSettings(Res.string.reader_action_options),
    OpenChapters(Res.string.reader_action_chapters),
    OpenInBrowser(Res.string.reader_action_web),
    ToggleBookmark(Res.string.reader_action_bookmark),
    PageMenu(Res.string.reader_action_page_menu),
    ToggleAutoScroll(Res.string.reader_action_toggle_auto),
    AutoScrollFaster(Res.string.reader_action_auto_faster),
    AutoScrollSlower(Res.string.reader_action_auto_slower),
    Close(Res.string.reader_action_close),
}

// How far the long scroll moves for a key or a tap. The arrow keys move by dp, and the page key and the tap move by a share of the height of the screen.
data class ScrollSteps(val arrowDp: Float = 100f, val pageFraction: Float = 0.85f, val tapFraction: Float = 0.6f) {
    companion object {
        val ArrowRange = 20f..600f
        val FractionRange = 0.1f..1f
    }
}
