package com.sumizuri.app.feature.settings.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.feature.browse.source.PicksKey
import com.sumizuri.app.feature.library.part.PicksOnlyPinnedKey
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_browse_picks
import com.sumizuri.app.shared.resources.suki_browse_picks_hint
import com.sumizuri.app.shared.resources.suki_library_picks_row
import com.sumizuri.app.shared.resources.suki_library_picks_rows
import com.sumizuri.app.shared.resources.suki_picks_from
import com.sumizuri.app.shared.resources.suki_picks_from_pinned
import com.sumizuri.app.shared.resources.suki_picks_from_recent
import com.sumizuri.app.shared.resources.suki_picks_hint
import com.sumizuri.app.shared.resources.suki_picks_in_library
import com.sumizuri.app.shared.resources.suki_picks_in_library_hint
import com.sumizuri.app.shared.resources.suki_picks_rows_title
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Where the recommendations show, how many rows the library has and which sources they come from.
@Composable
fun PicksRoute(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val prefs by container.libraryPrefs.prefs.collectAsStateWithLifecycle(LibraryPrefs())
    val inPopular by remember(container) { container.settings.flag(PicksKey, true) }.collectAsStateWithLifecycle(true)
    val onlyPinned by remember(container) {
        container.settings.flag(PicksOnlyPinnedKey, false)
    }.collectAsStateWithLifecycle(false)
    val scope = rememberCoroutineScope()
    val rows = prefs.picksRows
    fun setRows(n: Int) {
        scope.launch { container.libraryPrefs.update { it.withCount(LibrarySection.Picks, n) } }
    }
    SettingsPageFrame(Res.string.suki_library_picks_row, onBack, subtitle = Res.string.suki_picks_hint) {
        SettingsSection(Res.string.suki_library_picks_row) {
            padded {
                SwitchSetting(
                    Res.string.suki_picks_in_library,
                    rows > 0,
                    { on -> setRows(if (on) LibraryPrefs.DefaultPickRows else 0) },
                    Res.string.suki_picks_in_library_hint,
                )
            }
            if (rows > 0) {
                padded {
                    ChoiceSetting(Res.string.suki_picks_rows_title, (1..LibraryPrefs.MaxPickRows).toList(), rows, {
                        stringResource(Res.string.suki_library_picks_rows, it)
                    }) { setRows(it) }
                }
                padded {
                    ChoiceSetting(Res.string.suki_picks_from, listOf(false, true), onlyPinned, {
                        stringResource(if (it) Res.string.suki_picks_from_pinned else Res.string.suki_picks_from_recent)
                    }) { only -> scope.launch { container.settings.setFlag(PicksOnlyPinnedKey, only) } }
                }
            }
            padded {
                SwitchSetting(Res.string.suki_browse_picks, inPopular, { on ->
                    scope.launch { container.settings.setFlag(PicksKey, on) }
                }, Res.string.suki_browse_picks_hint)
            }
        }
    }
}
