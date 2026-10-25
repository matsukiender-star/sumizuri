package com.sumizuri.app.ui.kit.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cancel
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import org.jetbrains.compose.resources.stringResource

// The button that closes a dialog without doing what it asks.
@Composable
fun CancelButton(onClick: () -> Unit, enabled: Boolean = true) {
    InkOutlinedButton(onClick, enabled = enabled) { Text(stringResource(Res.string.cancel)) }
}

// A question before something is done: a title, what will happen, the button that does it and the one that cancels.
@Composable
fun ConfirmDialog(title: String, message: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { InkButton(onConfirm) { Text(confirm) } },
        dismissButton = { CancelButton(onDismiss) },
    )
}
