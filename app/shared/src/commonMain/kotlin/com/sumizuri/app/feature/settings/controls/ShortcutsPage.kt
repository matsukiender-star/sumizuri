package com.sumizuri.app.feature.settings.controls

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.controls.AppGestures
import com.sumizuri.app.data.model.controls.AppShortcut

private val Rows = AppShortcut.entries.map { KeyRow(it, it.label) }

// The keys that work anywhere in the app. They need Ctrl, Alt or a function key, so typing in a field never triggers one.
@Composable
internal fun ShortcutsPage(
    gestures: AppGestures,
    onChange: ((AppGestures) -> AppGestures) -> Unit,
    onBack: () -> Unit,
) {
    ControlsFrame(ControlsPage.Shortcuts, onBack) {
        KeyBindingsList(
            rows = Rows,
            keys = gestures.shortcuts,
            needsModifier = true,
            onAdd = { shortcut, chord -> onChange { it.withShortcut(shortcut, chord) } },
            onRemove = { shortcut, chord -> onChange { it.withoutShortcut(shortcut, chord) } },
            onReset = { shortcut -> onChange { it.withShortcutReset(shortcut) } },
            onResetAll = { onChange { it.copy(shortcuts = AppGestures.DefaultShortcuts) } },
        )
    }
}
