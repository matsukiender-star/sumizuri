package com.sumizuri.app.feature.settings.controls

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.key_record_hint
import com.sumizuri.app.shared.resources.key_record_need_modifier
import com.sumizuri.app.shared.resources.key_record_save
import com.sumizuri.app.shared.resources.key_record_taken
import com.sumizuri.app.shared.resources.key_record_title
import com.sumizuri.app.ui.controls.KeyChord
import com.sumizuri.app.ui.controls.isMouseKey
import com.sumizuri.app.ui.controls.mouseChords
import com.sumizuri.app.ui.controls.text
import com.sumizuri.app.ui.controls.toChord
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Waits for the person to press the key they want, shows it as they press, and says when another action holds it now. A shortcut that works anywhere in the app must have Ctrl, Alt or a function key, so that typing in a field is never caught.
@Composable
internal fun KeyRecorderDialog(
    action: String,
    takenBy: (KeyChord) -> StringResource?,
    needsModifier: Boolean,
    onRecorded: (KeyChord) -> Unit,
    onDismiss: () -> Unit,
) {
    var chord by remember { mutableStateOf<KeyChord?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val picked = chord
    val allowed =
        picked != null &&
            (
                !needsModifier || picked.ctrl || picked.alt || picked.meta ||
                    (picked.key.startsWith("F") && picked.key.length > 1) ||
                    isMouseKey(picked.key)
                )
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.key_record_title, action)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.key_record_hint), style = MaterialTheme.typography.bodyMedium)
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .mouseChords { pressed ->
                            chord = pressed
                            true
                        }.focusRequester(focus)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) event.toChord()?.let { chord = it }
                            true
                        },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            picked?.text().orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (picked != null && !allowed) {
                    Text(
                        stringResource(Res.string.key_record_need_modifier),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (allowed) {
                    takenBy(
                        picked,
                    )?.let { holder ->
                        Text(
                            stringResource(Res.string.key_record_taken, stringResource(holder)),
                            Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        },
        confirmButton = {
            InkButton(
                { picked?.let(onRecorded) },
                enabled = allowed,
            ) { Text(stringResource(Res.string.key_record_save)) }
        },
        dismissButton = { CancelButton(onDismiss) },
    )
}
