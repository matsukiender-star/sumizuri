package com.sumizuri.app.feature.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_close
import com.sumizuri.app.shared.resources.editor_compare
import com.sumizuri.app.shared.resources.editor_dark
import com.sumizuri.app.shared.resources.editor_light
import com.sumizuri.app.shared.resources.editor_preview_desktop
import com.sumizuri.app.shared.resources.editor_preview_phone
import com.sumizuri.app.shared.resources.editor_redo
import com.sumizuri.app.shared.resources.editor_save
import com.sumizuri.app.shared.resources.editor_saves_copy
import com.sumizuri.app.shared.resources.editor_undo
import com.sumizuri.app.ui.kit.art.InkButton
import org.jetbrains.compose.resources.stringResource

// The bar over the editor, in the colors of the app and not of the theme being made. A tap on the name opens the tab where it is changed, and a built in theme says Save keeps a copy.
@Composable
internal fun EditorBar(state: ThemeEditorState, desktop: Boolean?, onClose: () -> Unit, onSave: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(8.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.Outlined.Close, stringResource(Res.string.editor_close)) }
            Column(Modifier.weight(1f).clickable { state.open(EditorSection.Basics) }) {
                Text(
                    state.draft.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.original.isBuiltIn) {
                    Text(
                        stringResource(Res.string.editor_saves_copy),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            // A switch between the phone and the desktop layout of the preview, when there is room for the desktop one.
            if (desktop != null) {
                IconButton({ state.desktop = !desktop }) {
                    Icon(
                        if (desktop) Icons.Outlined.Computer else Icons.Outlined.PhoneAndroid,
                        stringResource(
                            if (desktop) Res.string.editor_preview_phone else Res.string.editor_preview_desktop,
                        ),
                    )
                }
            }
            IconButton({
                state.dark = !state.dark
            }) {
                Icon(
                    if (state.dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    stringResource(if (state.dark) Res.string.editor_light else Res.string.editor_dark),
                )
            }
            IconButton(state::undo, enabled = state.canUndo) {
                Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(Res.string.editor_undo))
            }
            IconButton(state::redo, enabled = state.canRedo) {
                Icon(Icons.AutoMirrored.Outlined.Redo, stringResource(Res.string.editor_redo))
            }
            Box(
                Modifier.padding(horizontal = 10.dp).pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        state.comparing = true
                        tryAwaitRelease()
                        state.comparing = false
                    })
                },
            ) {
                Icon(
                    Icons.Outlined.Visibility,
                    stringResource(Res.string.editor_compare),
                    tint = if (state.comparing) EditorBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            InkButton(
                onSave,
                enabled = state.changed || state.original.isBuiltIn,
            ) { Text(stringResource(Res.string.editor_save)) }
        }
    }
}
