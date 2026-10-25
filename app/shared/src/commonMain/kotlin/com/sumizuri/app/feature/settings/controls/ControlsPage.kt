package com.sumizuri.app.feature.settings.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardCommandKey
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.controls_gestures
import com.sumizuri.app.shared.resources.controls_gestures_hint
import com.sumizuri.app.shared.resources.controls_reader_keys
import com.sumizuri.app.shared.resources.controls_reader_keys_hint
import com.sumizuri.app.shared.resources.controls_shortcuts
import com.sumizuri.app.shared.resources.controls_shortcuts_hint
import com.sumizuri.app.shared.resources.controls_tap_zones
import com.sumizuri.app.shared.resources.controls_tap_zones_hint
import org.jetbrains.compose.resources.StringResource

// The pages of the controls, each opened from a row of the settings.
enum class ControlsPage(val title: StringResource, val hint: StringResource, val icon: ImageVector) {
    ReaderKeys(Res.string.controls_reader_keys, Res.string.controls_reader_keys_hint, Icons.Outlined.Keyboard),
    TapZones(Res.string.controls_tap_zones, Res.string.controls_tap_zones_hint, Icons.Outlined.TouchApp),
    Shortcuts(Res.string.controls_shortcuts, Res.string.controls_shortcuts_hint, Icons.Outlined.KeyboardCommandKey),
    Gestures(Res.string.controls_gestures, Res.string.controls_gestures_hint, Icons.Outlined.Gesture),
}
