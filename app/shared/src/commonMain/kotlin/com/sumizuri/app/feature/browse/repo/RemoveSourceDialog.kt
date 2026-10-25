package com.sumizuri.app.feature.browse.repo

import androidx.compose.runtime.Composable
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.source_remove_keep
import com.sumizuri.app.shared.resources.source_remove_message
import com.sumizuri.app.shared.resources.source_remove_title
import com.sumizuri.app.ui.kit.dialog.ConfirmDialog
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// Asks before a source is removed while saved titles still came from it, and says what becomes of them.
@Composable
internal fun RemoveSourceDialog(prompt: RemovePrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        stringResource(Res.string.source_remove_title, prompt.name),
        pluralStringResource(Res.plurals.source_remove_message, prompt.titles, prompt.titles),
        stringResource(Res.string.source_remove_keep),
        onConfirm,
        onDismiss,
    )
}
