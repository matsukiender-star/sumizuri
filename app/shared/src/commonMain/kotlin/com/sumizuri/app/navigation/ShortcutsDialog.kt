package com.sumizuri.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.sumizuri.app.ui.kit.dialog.AppDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.controls.AppGestures
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ok
import com.sumizuri.app.shared.resources.shortcut_help_title
import com.sumizuri.app.shared.resources.shortcut_unbound
import com.sumizuri.app.ui.controls.text
import com.sumizuri.app.ui.kit.art.InkButton
import org.jetbrains.compose.resources.stringResource

// The list of keys of the app and what each does, which opens with a key too. A shortcut with no key is shown as not set.
@Composable
internal fun ShortcutsDialog(gestures: AppGestures, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.shortcut_help_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((shortcut, keys) in gestures.shortcuts) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(shortcut.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (keys.isEmpty()) stringResource(Res.string.shortcut_unbound) else keys.map { it.text() }.joinToString("   "),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (keys.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        confirmButton = { InkButton(onDismiss) { Text(stringResource(Res.string.ok)) } },
    )
}
