package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.ProfilePreset
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.blocks_layouts
import com.sumizuri.app.shared.resources.preset_confirm_message
import com.sumizuri.app.shared.resources.preset_use
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import org.jetbrains.compose.resources.stringResource

// The layouts of the profile, all four in sight at once. A tap chooses one and the button puts it in place of the blocks, with a word that the blocks made by hand go with them.
@Composable
internal fun LayoutsDialog(onUse: (ProfilePreset) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf<ProfilePreset?>(null) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.blocks_layouts)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PresetPicker(chosen) { chosen = it }
                Text(
                    stringResource(Res.string.preset_confirm_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        },
        confirmButton = {
            InkButton({
                chosen?.let(onUse)
                onDismiss()
            }, enabled = chosen != null) { Text(stringResource(Res.string.preset_use)) }
        },
        dismissButton = { CancelButton(onDismiss) },
    )
}
