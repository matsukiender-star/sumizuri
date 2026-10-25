package com.sumizuri.app.feature.library.shelves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.DialogProperties
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibrarySortField
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.data.model.library.shelf.ShelfCondition
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.rule_own_order
import com.sumizuri.app.shared.resources.rule_status
import com.sumizuri.app.shared.resources.save
import com.sumizuri.app.shared.resources.shelves_delete
import com.sumizuri.app.shared.resources.shelves_edit
import com.sumizuri.app.shared.resources.shelves_every_kind
import com.sumizuri.app.shared.resources.shelves_exclude
import com.sumizuri.app.shared.resources.shelves_kind
import com.sumizuri.app.shared.resources.sort_ascending
import com.sumizuri.app.shared.resources.sort_by
import com.sumizuri.app.shared.resources.sort_descending
import com.sumizuri.app.shared.resources.sort_direction
import com.sumizuri.app.shared.resources.sort_then
import com.sumizuri.app.shared.resources.sort_then_direction
import com.sumizuri.app.shared.resources.sort_then_none
import com.sumizuri.app.shared.resources.status_any
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import com.sumizuri.app.ui.kit.input.AppSwitch
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import org.jetbrains.compose.resources.stringResource

// The statuses a shelf can be told to show, with null for all of them.
private val Statuses: List<Status?> = listOf(null, Status.Ongoing, Status.Completed, Status.Hiatus, Status.Cancelled)

// What a shelf can be changed to: its name, the kind of title it is for, the conditions that put titles on it by themselves, an order of its own with a second field for ties, the one status it shows, and whether its titles are left out of the check for new chapters, saved together, and a button to delete it that asks again before it does.
@Composable
internal fun ShelfDialog(
    shelf: Category,
    countMatches: (List<ShelfCondition>, Boolean) -> Int,
    onSave: (ShelfEdit) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(shelf.name) }
    var exclude by remember { mutableStateOf(shelf.excludeFromUpdate) }
    var kind by remember { mutableStateOf(shelf.kind) }
    var rule by remember { mutableStateOf(shelf.rule) }
    var status by remember { mutableStateOf(shelf.status) }
    var conditions by remember { mutableStateOf(shelf.conditions) }
    var matchAll by remember { mutableStateOf(shelf.matchAll) }
    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 620.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(Res.string.shelves_edit)) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true)
                ChoiceSetting(Res.string.shelves_kind, listOf<Kind?>(null) + Kind.entries, kind, {
                    it?.let { shown -> stringResource(shown.label) }
                        ?: stringResource(Res.string.shelves_every_kind)
                }) { kind = it }
                ConditionEditor(conditions, matchAll, countMatches(conditions, matchAll), { conditions = it }, {
                    matchAll =
                        it
                })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.shelves_exclude),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AppSwitch(exclude, { exclude = it })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.rule_own_order),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AppSwitch(rule != null, { on -> rule = if (on) SortRule() else null })
                }
                rule?.let { own ->
                    ChoiceSetting(
                        Res.string.sort_by,
                        LibrarySortField.entries,
                        own.sort,
                        { stringResource(it.label) },
                    ) {
                        rule =
                            own.copy(sort = it, then = own.then?.takeIf { next -> next != it })
                    }
                    ChoiceSetting(Res.string.sort_direction, listOf(true, false), own.ascending, {
                        stringResource(if (it) Res.string.sort_ascending else Res.string.sort_descending)
                    }) {
                        rule =
                            own.copy(ascending = it)
                    }
                    ChoiceSetting(
                        Res.string.sort_then,
                        listOf<LibrarySortField?>(null) + LibrarySortField.entries.filter { it != own.sort },
                        own.then,
                        {
                            it?.let { field -> stringResource(field.label) }
                                ?: stringResource(Res.string.sort_then_none)
                        },
                    ) { rule = own.copy(then = it) }
                    if (own.then !=
                        null
                    ) {
                        ChoiceSetting(Res.string.sort_then_direction, listOf(true, false), own.thenAscending, {
                            stringResource(if (it) Res.string.sort_ascending else Res.string.sort_descending)
                        }) {
                            rule =
                                own.copy(thenAscending = it)
                        }
                    }
                }
                ChoiceSetting(Res.string.rule_status, Statuses, status, {
                    it?.let { shown -> stringResource(shown.label) }
                        ?: stringResource(Res.string.status_any)
                }) { status = it }
                TextButton(
                    onDelete,
                ) { Text(stringResource(Res.string.shelves_delete), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            InkButton({
                onSave(ShelfEdit(name, exclude, kind, rule, status, conditions, matchAll))
            }, enabled = name.isNotBlank()) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { CancelButton(onDismiss) },
    )
}
