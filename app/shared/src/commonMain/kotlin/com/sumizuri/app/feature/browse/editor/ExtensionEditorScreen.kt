package com.sumizuri.app.feature.browse.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sumizuri.app.core.log.LogEntry
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_save
import com.sumizuri.app.shared.resources.ext_edit_new_title
import com.sumizuri.app.shared.resources.ext_edit_saved
import com.sumizuri.app.shared.resources.ext_edit_tab_code
import com.sumizuri.app.shared.resources.ext_edit_tab_details
import com.sumizuri.app.shared.resources.ext_edit_tab_test
import com.sumizuri.app.shared.resources.ext_edit_unsaved
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.chips.FilterTabs
import com.sumizuri.app.ui.kit.cover.SourceMark
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.page.PageTopBar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// A window at least this wide has the code and the panels side by side, and the panels take this much of it.
private val SideBySideWidth = 900.dp
private val SidePanelWidth = 440.dp

// The parts of the editor. A phone shows one at a time, and a wide window shows the code with one of the others beside it.
enum class EditorTab(val label: StringResource) {
    Code(Res.string.ext_edit_tab_code),
    Details(Res.string.ext_edit_tab_details),
    Test(Res.string.ext_edit_tab_test),
}

// The editor: a bar with the name, whether it is saved and Save, then the code, the details of the source and its tests.
@Composable
fun ExtensionEditorScreen(
    state: ExtensionEditorState,
    console: List<LogEntry>,
    field: TextFieldValue,
    onField: (TextFieldValue) -> Unit,
    actions: ExtensionEditorActions,
) {
    var tab by rememberSaveable { mutableStateOf(EditorTab.Code) }
    Column(Modifier.fillMaxSize()) {
        EditorTopBar(state, actions)
        state.problem?.let {
            Text(
                it,
                Modifier.padding(horizontal = PageGutter, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= SideBySideWidth) {
                // The side shows the tests until the details are chosen, as the code is always there.
                val side = if (tab == EditorTab.Code) EditorTab.Test else tab
                Row(
                    Modifier.fillMaxSize().padding(start = PageGutter, end = PageGutter, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CodeEditor(field, onField, Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(SidePanelWidth).fillMaxHeight()) {
                        FilterTabs(
                            listOf(
                                EditorTab.Details,
                                EditorTab.Test,
                            ).map { it to stringResource(it.label) },
                            side,
                            {
                                tab =
                                    it
                            },
                        )
                        EditorPanel(side, state, console, actions, PaddingValues(top = 12.dp), Modifier.weight(1f))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    FilterTabs(
                        EditorTab.entries.map {
                            it to stringResource(it.label)
                        },
                        tab,
                        { tab = it },
                        Modifier.padding(horizontal = PageGutter),
                    )
                    if (tab == EditorTab.Code) {
                        CodeEditor(
                            field,
                            onField,
                            Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        )
                    } else {
                        EditorPanel(
                            tab,
                            state,
                            console,
                            actions,
                            PaddingValues(start = PageGutter, end = PageGutter, top = 12.dp, bottom = 16.dp),
                            Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorPanel(
    tab: EditorTab,
    state: ExtensionEditorState,
    console: List<LogEntry>,
    actions: ExtensionEditorActions,
    padding: PaddingValues,
    modifier: Modifier,
) {
    when (tab) {
        EditorTab.Details -> EditorDetailsPanel(state, actions, padding, modifier.fillMaxWidth())
        else -> EditorTestPanel(state, console, actions, padding, modifier.fillMaxWidth())
    }
}

// The bar: the icon and name of the source, whether it is saved, and Save.
@Composable
private fun EditorTopBar(state: ExtensionEditorState, actions: ExtensionEditorActions) {
    val saved = when {
        state.dirty -> stringResource(Res.string.ext_edit_unsaved)
        state.savedAs != null -> stringResource(Res.string.ext_edit_saved)
        else -> null
    }
    PageTopBar(
        title = state.name.ifBlank { stringResource(Res.string.ext_edit_new_title) },
        subtitle = saved,
        onBack = actions.onClose,
        leading = { SourceMark(state.iconUrl, size = 36.dp) },
    ) {
        InkButton(actions.onSave, Modifier.padding(horizontal = 8.dp), enabled = state.canSave) {
            if (state.saving) {
                InkCircularProgressIndicator(
                    Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
            } else {
                Text(stringResource(Res.string.editor_save))
            }
        }
    }
}
