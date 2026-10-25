package com.sumizuri.app.feature.title.chapter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.chapters_gap_inline
import com.sumizuri.app.shared.resources.chapters_missing
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// A break in the chapter list, shown between the two chapters it falls between, as a line, what is missing and a line.
@Composable
internal fun GapMarker(missing: List<Int>) {
    val error = MaterialTheme.colorScheme.error
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val line = Modifier.weight(1f)
        HorizontalDivider(line, color = error.copy(alpha = 0.35f))
        Icon(Icons.Outlined.WarningAmber, null, Modifier.padding(start = 10.dp, end = 4.dp).size(14.dp), tint = error)
        Text(
            stringResource(Res.string.chapters_gap_inline, missingRanges(missing)),
            Modifier.padding(end = 10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = error,
        )
        HorizontalDivider(line, color = error.copy(alpha = 0.35f))
    }
}

// One line over the grid that says how many chapters are missing and which, since a grid has no spot between two chapters.
@Composable
internal fun MissingSummary(missing: List<Int>) {
    val error = MaterialTheme.colorScheme.error
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.WarningAmber, null, Modifier.padding(end = 6.dp, top = 2.dp).size(15.dp), tint = error)
        Text(
            pluralStringResource(Res.plurals.chapters_missing, missing.size, missing.size) + ": " +
                missingRanges(missing),
            style = MaterialTheme.typography.bodySmall,
            color = error,
        )
    }
}
