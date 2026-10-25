package com.sumizuri.app.feature.settings.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.controls.ReaderAction
import com.sumizuri.app.data.model.controls.ReaderControls
import com.sumizuri.app.data.model.controls.ScrollSteps
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.long_tap_menu
import com.sumizuri.app.shared.resources.long_tap_menu_hint
import com.sumizuri.app.shared.resources.scroll_arrow
import com.sumizuri.app.shared.resources.scroll_page
import com.sumizuri.app.shared.resources.scroll_steps_title
import com.sumizuri.app.shared.resources.scroll_tap
import com.sumizuri.app.shared.resources.volume_keys
import com.sumizuri.app.shared.resources.volume_keys_hint
import com.sumizuri.app.shared.resources.volume_keys_invert
import com.sumizuri.app.shared.resources.volume_keys_invert_hint
import com.sumizuri.app.shared.resources.volume_keys_title
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SliderSetting
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import kotlin.math.roundToInt

private val Rows = ReaderAction.entries.map { KeyRow(it, it.label) }

// The keys of the reader, one list for every action, and under it how far the long scroll moves for a key or a tap.
@Composable
internal fun ReaderKeysPage(
    controls: ReaderControls,
    onChange: ((ReaderControls) -> ReaderControls) -> Unit,
    onBack: () -> Unit,
) {
    ControlsFrame(ControlsPage.ReaderKeys, onBack) {
        KeyBindingsList(
            rows = Rows,
            keys = controls.keys,
            needsModifier = false,
            onAdd = { action, chord -> onChange { it.withKey(action, chord) } },
            onRemove = { action, chord -> onChange { it.withoutKey(action, chord) } },
            onReset = { action -> onChange { it.withKeysReset(action) } },
            onResetAll = { onChange { it.withAllKeysReset() } },
        )
        SettingsSection(Res.string.volume_keys_title) {
            padded {
                SwitchSetting(Res.string.volume_keys, controls.volumeKeys, { on ->
                    onChange {
                        it.withVolumeKeys(on)
                    }
                }, Res.string.volume_keys_hint)
            }
            padded {
                SwitchSetting(Res.string.volume_keys_invert, controls.volumeInverted, { on ->
                    onChange {
                        it.withVolumeInverted(on)
                    }
                }, Res.string.volume_keys_invert_hint, enabled = controls.volumeKeys)
            }
        }
        SettingsSection {
            padded {
                SwitchSetting(Res.string.long_tap_menu, controls.longTapMenu, { on ->
                    onChange {
                        it.withLongTapMenu(on)
                    }
                }, Res.string.long_tap_menu_hint)
            }
        }
        val steps = controls.scroll
        SettingsSection(Res.string.scroll_steps_title) {
            padded {
                SliderSetting(Res.string.scroll_arrow, steps.arrowDp, ScrollSteps.ArrowRange, {
                    "${it.roundToInt()} dp"
                }) { value -> onChange { it.withScroll(it.scroll.copy(arrowDp = value)) } }
            }
            padded {
                SliderSetting(Res.string.scroll_page, steps.pageFraction, ScrollSteps.FractionRange, {
                    "${(it * 100).roundToInt()} %"
                }) { value -> onChange { it.withScroll(it.scroll.copy(pageFraction = value)) } }
            }
            padded {
                SliderSetting(Res.string.scroll_tap, steps.tapFraction, ScrollSteps.FractionRange, {
                    "${(it * 100).roundToInt()} %"
                }) { value -> onChange { it.withScroll(it.scroll.copy(tapFraction = value)) } }
            }
        }
    }
}
