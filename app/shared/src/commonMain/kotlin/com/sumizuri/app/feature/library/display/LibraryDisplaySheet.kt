package com.sumizuri.app.feature.library.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cancel
import com.sumizuri.app.shared.resources.display_shelves
import com.sumizuri.app.shared.resources.display_title
import com.sumizuri.app.ui.kit.card.AppListRow
import com.sumizuri.app.ui.kit.page.AppSheet
import org.jetbrains.compose.resources.stringResource

// The look of the library over the page, with a way to the page where the shelves are changed. The sheet is a panel in the middle on a wide window. Each choice is saved as it is made.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryDisplaySheet(
    prefs: LibraryPrefs,
    onChange: ((LibraryPrefs) -> LibraryPrefs) -> Unit,
    onEditShelves: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shelves = @Composable {
        AppListRow(
            title = stringResource(Res.string.display_shelves),
            leading = {
                Icon(
                    Icons.AutoMirrored.Outlined.Label,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                onDismiss()
                onEditShelves()
            },
        )
    }
    AppSheet(onDismiss, title = stringResource(Res.string.display_title)) {
        LibraryDisplayBody(prefs, onChange)
        shelves()
    }
}
