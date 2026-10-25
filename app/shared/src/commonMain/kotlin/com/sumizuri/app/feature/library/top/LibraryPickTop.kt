package com.sumizuri.app.feature.library.top

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.library.LibraryPickCalls
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.selection_all
import com.sumizuri.app.shared.resources.selection_close
import com.sumizuri.app.shared.resources.selection_count
import com.sumizuri.app.shared.resources.selection_invert
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The bar over the top while titles are picked: the count, and the way to pick all, flip the picks or stop.
@Composable
internal fun LibraryPickTop(count: Int, total: Int, calls: LibraryPickCalls, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(calls.onClose) { Icon(Icons.Outlined.Close, stringResource(Res.string.selection_close)) }
            Text(
                pluralStringResource(Res.plurals.selection_count, count, count),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(calls.onSelectAll) {
                Icon(
                    if (count ==
                        total
                    ) {
                        Icons.Outlined.Deselect
                    } else {
                        Icons.Outlined.SelectAll
                    },
                    stringResource(Res.string.selection_all),
                )
            }
            IconButton(calls.onInvert) { Icon(Icons.Outlined.FlipToBack, stringResource(Res.string.selection_invert)) }
        }
    }
}
