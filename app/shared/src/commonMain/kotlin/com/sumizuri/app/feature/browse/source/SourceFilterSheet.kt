package com.sumizuri.app.feature.browse.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.browse_filters
import com.sumizuri.app.shared.resources.browse_filters_apply
import com.sumizuri.app.shared.resources.browse_filters_reset
import com.sumizuri.app.source.model.FilterGroup
import com.sumizuri.app.source.model.FilterType
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.page.AppSheet
import com.sumizuri.app.ui.kit.setting.ChipsSetting
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import org.jetbrains.compose.resources.stringResource

// The filters a source offers in one sheet, a choice of one, a choice of several, or a switch, and Apply runs the search with them. Filters next to each other share a card, and a choice of several, which is often a long list of genres, has a card of its own.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceFilterSheet(
    groups: List<FilterGroup>,
    current: Map<String, Any?>,
    onApply: (Map<String, Any?>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember(groups) {
        mutableStateOf(
            groups.associate {
                it.key to
                    (current[it.key] ?: it.defaultSelection())
            },
        )
    }
    val cards = remember(groups) { cardsOf(groups) }
    AppSheet(onDismiss, spacing = 12.dp, title = stringResource(Res.string.browse_filters)) {
        for (card in cards) {
            SettingsSection {
                for (group in card) {
                    padded {
                        when (group.type) {
                            FilterType.Select -> ChoiceSetting(
                                group.name,
                                group.options.map { it.value },
                                picked[group.key] as? String,
                                { value ->
                                    group.options
                                        .firstOrNull { it.value == value }
                                        ?.label
                                        .orEmpty()
                                },
                            ) { value -> picked = picked + (group.key to value) }

                            FilterType.MultiSelect -> {
                                val now = (picked[group.key] as? List<*>).orEmpty().map { it.toString() }
                                ChipsSetting(
                                    group.name,
                                    group.options.map {
                                        it.value to it.label
                                    },
                                    now.toSet(),
                                    { value ->
                                        picked =
                                            picked + (group.key to if (value in now) now - value else now + value)
                                    },
                                )
                            }

                            FilterType.Checkbox -> SwitchSetting(group.name, picked[group.key] == true, { value ->
                                picked =
                                    picked + (group.key to value)
                            })
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton({
                picked = groups.associate {
                    it.key to it.defaultSelection()
                }
            }) { Text(stringResource(Res.string.browse_filters_reset)) }
            InkButton({
                onApply(picked)
                onDismiss()
            }) { Text(stringResource(Res.string.browse_filters_apply)) }
        }
    }
}

// The filters in cards, in the order the source gives them: a run of single choices and switches shares one card, and each choice of several has its own.
private fun cardsOf(groups: List<FilterGroup>): List<List<FilterGroup>> {
    val cards = mutableListOf<MutableList<FilterGroup>>()
    for (group in groups) {
        val alone = group.type == FilterType.MultiSelect
        val last = cards.lastOrNull()
        if (last == null || alone ||
            last.first().type == FilterType.MultiSelect
        ) {
            cards += mutableListOf(group)
        } else {
            last += group
        }
    }
    return cards
}
