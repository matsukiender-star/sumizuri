package com.sumizuri.app.feature.browse.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.core.log.LogEntry
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_discard
import com.sumizuri.app.shared.resources.editor_discard_title
import com.sumizuri.app.shared.resources.editor_keep_editing
import com.sumizuri.app.shared.resources.ext_edit_discard_message
import com.sumizuri.app.source.model.CommentSort
import com.sumizuri.app.source.model.SourceCatalog
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import org.jetbrains.compose.resources.stringResource

// What the screen of the editor asks for.
class ExtensionEditorActions(
    val onClose: () -> Unit,
    val onSave: () -> Unit,
    val onName: (String) -> Unit,
    val onLang: (String) -> Unit,
    val onKind: (Kind) -> Unit,
    val onBaseUrl: (String) -> Unit,
    val onIconUrl: (String) -> Unit,
    val onFillFromCode: () -> Unit,
    val onInfo: () -> Unit,
    val onPopular: () -> Unit,
    val onLatest: () -> Unit,
    val onSearch: (String) -> Unit,
    val onNextPage: () -> Unit,
    val onOpenEntry: (SourceEntry) -> Unit,
    val onOpenChapter: (SourceChapter) -> Unit,
    val onComments: (CommentSort) -> Unit,
    val onChapterComments: (CommentSort) -> Unit,
    val onFilters: () -> Unit,
    val onCatalogs: () -> Unit,
    val onOpenCatalog: (SourceCatalog) -> Unit,
    val onSettings: () -> Unit,
    val onRunAll: () -> Unit,
    val onStopTest: () -> Unit,
    val onTestBack: () -> Unit,
    val onShowView: (TestView) -> Unit,
    val onClearConsole: () -> Unit,
)

// The editor of an extension, opened on an installed source or on a template for a new one. Leaving with changes that are not saved asks first.
@Composable
fun ExtensionEditorRoute(sourceId: String?, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "extension_editor_" + (sourceId ?: "new")) {
        ExtensionEditorViewModel(sourceId, container.sources, container.manager)
    }
    DisposableEffect(viewModel) {
        viewModel.onEnter()
        onDispose { viewModel.onLeave() }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val console by viewModel.console.collectAsStateWithLifecycle()
    var leaving by remember { mutableStateOf(false) }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = state.dirty) {
        leaving =
            true
    }
    val actions = remember(viewModel, onBack) {
        ExtensionEditorActions(
            onClose = { if (viewModel.state.value.dirty) leaving = true else onBack() },
            onSave = viewModel::onSave,
            onName = viewModel::onName,
            onLang = viewModel::onLang,
            onKind = viewModel::onKind,
            onBaseUrl = viewModel::onBaseUrl,
            onIconUrl = viewModel::onIconUrl,
            onFillFromCode = viewModel::onFillFromCode,
            onInfo = viewModel::onInfo,
            onPopular = viewModel::onPopular,
            onLatest = viewModel::onLatest,
            onSearch = viewModel::onSearch,
            onNextPage = viewModel::onNextPage,
            onOpenEntry = viewModel::onOpenEntry,
            onOpenChapter = viewModel::onOpenChapter,
            onComments = viewModel::onComments,
            onChapterComments = viewModel::onChapterComments,
            onFilters = viewModel::onFilters,
            onCatalogs = viewModel::onCatalogs,
            onOpenCatalog = viewModel::onOpenCatalog,
            onSettings = viewModel::onSettings,
            onRunAll = viewModel::onRunAll,
            onStopTest = viewModel::onStopTest,
            onTestBack = viewModel::onTestBack,
            onShowView = viewModel::onShowView,
            onClearConsole = viewModel::onClearConsole,
        )
    }
    if (state.loaded) {
        EditorLoaded(state, console, actions, viewModel::onCode)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { InkCircularProgressIndicator() }
    }
    if (leaving) {
        AppDialog(
            onDismissRequest = { leaving = false },
            title = { Text(stringResource(Res.string.editor_discard_title)) },
            text = { Text(stringResource(Res.string.ext_edit_discard_message)) },
            confirmButton = {
                InkButton({
                    leaving = false
                    onBack()
                }) { Text(stringResource(Res.string.editor_discard)) }
            },
            dismissButton = {
                InkOutlinedButton(
                    { leaving = false },
                ) { Text(stringResource(Res.string.editor_keep_editing)) }
            },
        )
    }
}

// The code with its cursor is kept here once it is read, so it stays where it was when the tabs of a phone change.
@Composable
private fun EditorLoaded(
    state: ExtensionEditorState,
    console: List<LogEntry>,
    actions: ExtensionEditorActions,
    onCode: (String) -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(state.code)) }
    ExtensionEditorScreen(state, console, field, {
        field = it
        onCode(it.text)
    }, actions)
}
