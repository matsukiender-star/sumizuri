package com.sumizuri.app.feature.library.shelves

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.library.shelf.ShelfCondition
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cond_add
import com.sumizuri.app.shared.resources.cond_heading
import com.sumizuri.app.shared.resources.cond_hint
import com.sumizuri.app.shared.resources.cond_match_all
import com.sumizuri.app.shared.resources.cond_match_any
import com.sumizuri.app.shared.resources.cond_matches
import com.sumizuri.app.ui.kit.setting.PartTabs
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The conditions that put titles on a shelf by themselves, one row for each, with a choice of whether a title must meet all of them or any one, a button for another condition, and how many titles meet them now. Rows with no value are kept while they are being filled in, and left out when the shelf is used.
@Composable
internal fun ConditionEditor(
    conditions: List<ShelfCondition>,
    matchAll: Boolean,
    matches: Int,
    onConditions: (List<ShelfCondition>) -> Unit,
    onMatchAll: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.cond_heading), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(Res.string.cond_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        ShelfTemplates { picked ->
            onConditions(picked)
            onMatchAll(true)
        }
        if (conditions.size > 1) {
            PartTabs(listOf(true, false), matchAll, {
                stringResource(if (it) Res.string.cond_match_all else Res.string.cond_match_any)
            }, onSelect = onMatchAll)
        }
        conditions.forEachIndexed { index, condition ->
            ConditionRow(
                condition,
                onChange = { changed -> onConditions(conditions.toMutableList().also { it[index] = changed }) },
                onRemove = { onConditions(conditions.filterIndexed { at, _ -> at != index }) },
            )
        }
        TextButton({ onConditions(conditions + ShelfCondition()) }) { Text(stringResource(Res.string.cond_add)) }
        if (conditions.any { it.complete }) {
            Text(
                pluralStringResource(Res.plurals.cond_matches, matches, matches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
