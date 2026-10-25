package com.sumizuri.app.feature.library.display

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.library.LibrarySortField
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.feature.library.LibraryState
import com.sumizuri.app.feature.library.UncategorizedShelf
import com.sumizuri.app.feature.library.kindRuleKey
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.all
import com.sumizuri.app.shared.resources.library_sort
import com.sumizuri.app.shared.resources.rule_own_order
import com.sumizuri.app.shared.resources.rule_status
import com.sumizuri.app.shared.resources.shelf_none
import com.sumizuri.app.shared.resources.sort_ascending
import com.sumizuri.app.shared.resources.sort_by
import com.sumizuri.app.shared.resources.sort_descending
import com.sumizuri.app.shared.resources.sort_direction
import com.sumizuri.app.shared.resources.sort_pin_favorites
import com.sumizuri.app.shared.resources.sort_pin_unread
import com.sumizuri.app.shared.resources.sort_scope_default
import com.sumizuri.app.shared.resources.sort_then
import com.sumizuri.app.shared.resources.sort_then_direction
import com.sumizuri.app.shared.resources.sort_then_none
import com.sumizuri.app.shared.resources.status_any
import com.sumizuri.app.ui.kit.page.AppSheet
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.PartTabs
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import org.jetbrains.compose.resources.stringResource

// The places an order can be set for, from the widest to the narrowest: everywhere, the open kind, and the open tab, which is all the titles, the titles on no shelf or one shelf.
private enum class Place { Default, Kind, Tab }

// The statuses a shelf can be told to show, with null for all of them.
private val Statuses: List<Status?> = listOf(null, Status.Ongoing, Status.Completed, Status.Hiatus, Status.Cancelled)

// The order of the library, set for where it applies. The default order is used wherever nothing narrower has one, and the open kind and the open tab, which is all the titles, the titles on no shelf or a shelf, can each have an order of their own. An order is a field with a direction and a second field for ties, and favourites and unread titles can be put first in every order. A shelf can also show only one status. Each choice is saved as it is made.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySortSheet(
    state: LibraryState,
    onPrefs: ((LibraryPrefs) -> LibraryPrefs) -> Unit,
    onShelfRule: (Long, SortRule?, Status?) -> Unit,
    onDismiss: () -> Unit,
) {
    val prefs = state.prefs
    val shelf = state.shelf
    val kind = state.kind
    // The tabs of all the titles and of the titles on no shelf keep their order with the other choices, a shelf keeps its own.
    val tabKey = when (prefs.category) {
        null -> "all"
        UncategorizedShelf -> "none"
        else -> null
    }
    fun ownOf(place: Place): SortRule? = when (place) {
        Place.Default -> prefs.defaultRule
        Place.Kind -> kind?.let { prefs.rules[kindRuleKey(it)] }
        Place.Tab -> if (tabKey != null) prefs.rules[tabKey] else shelf?.rule
    }
    val places = buildList {
        add(Place.Default)
        if (kind != null) add(Place.Kind)
        add(Place.Tab)
    }
    // It opens on the narrowest place that has an order, which is the one in use.
    var place by remember { mutableStateOf(places.last { it == Place.Default || ownOf(it) != null }) }
    val own = ownOf(place)
    val rule = own ?: state.rule
    val noShelf = stringResource(Res.string.shelf_none)
    val all = stringResource(Res.string.all)
    val kindName = kind?.let { stringResource(it.label) }
    val names = places.map { entry ->
        entry to when (entry) {
            Place.Default -> stringResource(Res.string.sort_scope_default)
            Place.Kind -> kindName.orEmpty()
            Place.Tab -> shelf?.name ?: if (tabKey == "none") noShelf else all
        }
    }

    // Keeps a new order for the place, or takes the order of the place away with null.
    fun save(next: SortRule?) {
        val key = when (place) {
            Place.Default -> null
            Place.Kind -> kind?.let { kindRuleKey(it) }
            Place.Tab -> tabKey
        }
        when {
            place == Place.Default && next != null -> onPrefs {
                it.copy(
                    sort = next.sort,
                    ascending = next.ascending,
                    thenSort = next.then,
                    thenAscending = next.thenAscending,
                )
            }

            key != null -> onPrefs { it.copy(rules = if (next == null) it.rules - key else it.rules + (key to next)) }

            shelf != null -> onShelfRule(shelf.id, next, shelf.status)
        }
    }

    AppSheet(onDismiss, spacing = 12.dp, title = stringResource(Res.string.library_sort)) {
        if (places.size >
            1
        ) {
            PartTabs(places, place, { entry -> names.first { it.first == entry }.second }) { place = it }
        }
        if (place != Place.Default) {
            SettingsSection {
                padded {
                    SwitchSetting(
                        Res.string.rule_own_order,
                        own != null,
                        { on -> save(if (on) state.rule else null) },
                    )
                }
            }
        }
        if (place == Place.Default || own != null) {
            SettingsSection {
                padded {
                    ChoiceSetting(
                        Res.string.sort_by,
                        LibrarySortField.entries,
                        rule.sort,
                        { stringResource(it.label) },
                    ) {
                        save(
                            rule.copy(
                                sort = it,
                                then = rule.then?.takeIf { next ->
                                    next !=
                                        it
                                },
                            ),
                        )
                    }
                }
                padded {
                    ChoiceSetting(Res.string.sort_direction, listOf(true, false), rule.ascending, {
                        stringResource(if (it) Res.string.sort_ascending else Res.string.sort_descending)
                    }) { save(rule.copy(ascending = it)) }
                }
                padded {
                    ChoiceSetting(
                        Res.string.sort_then,
                        listOf<LibrarySortField?>(null) + LibrarySortField.entries.filter { it != rule.sort },
                        rule.then,
                        {
                            it?.let { field ->
                                stringResource(
                                    field.label,
                                )
                            } ?: stringResource(Res.string.sort_then_none)
                        },
                    ) { save(rule.copy(then = it)) }
                }
                if (rule.then != null) {
                    padded {
                        ChoiceSetting(Res.string.sort_then_direction, listOf(true, false), rule.thenAscending, {
                            stringResource(if (it) Res.string.sort_ascending else Res.string.sort_descending)
                        }) { save(rule.copy(thenAscending = it)) }
                    }
                }
            }
        }
        if (shelf != null && place == Place.Tab) {
            SettingsSection {
                padded {
                    ChoiceSetting(Res.string.rule_status, Statuses, shelf.status, {
                        stringResource(it?.label ?: Res.string.status_any)
                    }) { onShelfRule(shelf.id, shelf.rule, it) }
                }
            }
        }
        SettingsSection {
            padded {
                SwitchSetting(
                    Res.string.sort_pin_favorites,
                    prefs.pinFavorites,
                    { on -> onPrefs { it.copy(pinFavorites = on) } },
                )
            }
            padded {
                SwitchSetting(
                    Res.string.sort_pin_unread,
                    prefs.pinUnread,
                    { on -> onPrefs { it.copy(pinUnread = on) } },
                )
            }
        }
    }
}
