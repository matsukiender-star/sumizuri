package com.sumizuri.app.feature.settings.controls

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.controls.AppGestures
import com.sumizuri.app.data.model.controls.ChapterRowAction
import com.sumizuri.app.data.model.controls.LibraryCoverAction
import com.sumizuri.app.data.model.controls.NavDoubleTapAction
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.gesture_group_chapters
import com.sumizuri.app.shared.resources.gesture_group_covers
import com.sumizuri.app.shared.resources.gesture_group_tabs
import com.sumizuri.app.shared.resources.gesture_library_double_tap
import com.sumizuri.app.shared.resources.gesture_library_long_press
import com.sumizuri.app.shared.resources.gesture_library_tap
import com.sumizuri.app.shared.resources.gesture_long_press
import com.sumizuri.app.shared.resources.gesture_nav_double
import com.sumizuri.app.shared.resources.gesture_swipe_left
import com.sumizuri.app.shared.resources.gesture_swipe_right
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SettingsSection
import org.jetbrains.compose.resources.stringResource

// What a tap, a long press and a double tap on a cover of the library do, what a swipe or a long press on a chapter does, and what a second tap on the open tab does, in a card for each place.
@Composable
internal fun GesturesPage(gestures: AppGestures, onChange: ((AppGestures) -> AppGestures) -> Unit, onBack: () -> Unit) {
    ControlsFrame(ControlsPage.Gestures, onBack) {
        SettingsSection(Res.string.gesture_group_covers) {
            padded { ChoiceSetting(Res.string.gesture_library_tap, LibraryCoverAction.entries, gestures.libraryTap, { stringResource(it.label) }) { value -> onChange { it.copy(libraryTap = value) } } }
            padded { ChoiceSetting(Res.string.gesture_library_long_press, LibraryCoverAction.entries, gestures.libraryLongPress, { stringResource(it.label) }) { value -> onChange { it.copy(libraryLongPress = value) } } }
            padded { ChoiceSetting(Res.string.gesture_library_double_tap, LibraryCoverAction.entries, gestures.libraryDoubleTap, { stringResource(it.label) }) { value -> onChange { it.copy(libraryDoubleTap = value) } } }
        }
        SettingsSection(Res.string.gesture_group_chapters) {
            padded { ChoiceSetting(Res.string.gesture_swipe_right, ChapterRowAction.entries, gestures.chapterSwipeRight, { stringResource(it.label) }) { value -> onChange { it.copy(chapterSwipeRight = value) } } }
            padded { ChoiceSetting(Res.string.gesture_swipe_left, ChapterRowAction.entries, gestures.chapterSwipeLeft, { stringResource(it.label) }) { value -> onChange { it.copy(chapterSwipeLeft = value) } } }
            padded { ChoiceSetting(Res.string.gesture_long_press, ChapterRowAction.entries, gestures.chapterLongPress, { stringResource(it.label) }) { value -> onChange { it.copy(chapterLongPress = value) } } }
        }
        SettingsSection(Res.string.gesture_group_tabs) {
            padded { ChoiceSetting(Res.string.gesture_nav_double, NavDoubleTapAction.entries, gestures.navDoubleTap, { stringResource(it.label) }) { value -> onChange { it.copy(navDoubleTap = value) } } }
        }
    }
}
