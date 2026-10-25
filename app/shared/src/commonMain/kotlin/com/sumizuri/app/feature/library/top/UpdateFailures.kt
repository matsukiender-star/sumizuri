package com.sumizuri.app.feature.library.top

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.sync.UpdateFailure
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.connection_check_again
import com.sumizuri.app.shared.resources.connection_lost_notice
import com.sumizuri.app.shared.resources.failures_dismiss
import com.sumizuri.app.shared.resources.failures_notice
import com.sumizuri.app.shared.resources.failures_show
import com.sumizuri.app.shared.resources.failures_title
import com.sumizuri.app.ui.kit.card.AppListRow
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.page.AppSheet
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// A note that some titles could not be asked at the last check, with a way to see which and to dismiss it.
@Composable
internal fun FailuresNotice(count: Int, onShow: () -> Unit, onDismiss: () -> Unit) {
    AppListRow(
        title = pluralStringResource(Res.plurals.failures_notice, count, count),
        leading = { Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
        trailing = {
            TextButton(onShow) { Text(stringResource(Res.string.failures_show)) }
            TextButton(onDismiss) { Text(stringResource(Res.string.failures_dismiss)) }
        },
    )
}

// Says the last check stopped because the connection was lost, with how many titles it left unchecked and a button to check again.
@Composable
internal fun ConnectionLostNotice(count: Int, onCheckAgain: () -> Unit, onDismiss: () -> Unit) {
    AppListRow(
        title = pluralStringResource(Res.plurals.connection_lost_notice, count, count),
        leading = { Icon(Icons.Outlined.WifiOff, null, tint = MaterialTheme.colorScheme.error) },
        trailing = {
            TextButton(onCheckAgain) { Text(stringResource(Res.string.connection_check_again)) }
            TextButton(onDismiss) { Text(stringResource(Res.string.failures_dismiss)) }
        },
    )
}

// The titles that could not be asked, each with what went wrong. A tap opens the title, where the person can ask again or move it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateFailuresSheet(failures: List<UpdateFailure>, onOpen: (Long) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismiss, spacing = 8.dp, title = stringResource(Res.string.failures_title)) {
        // The failures of one source come together under its name, the source with the most first, so a site that is down shows as one group.
        for ((source, group) in failures.groupBy { it.source }.entries.sortedByDescending { it.value.size }) {
            if (source.isNotBlank()) AppSectionLabel("$source · ${group.size}")
            for (failure in group) {
                AppListRow(failure.name, subtitle = failure.problem, onClick = {
                    onDismiss()
                    onOpen(failure.titleId)
                })
            }
        }
    }
}
