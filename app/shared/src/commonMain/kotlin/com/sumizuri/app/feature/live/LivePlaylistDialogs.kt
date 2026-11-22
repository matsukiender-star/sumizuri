package com.sumizuri.app.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.live.LiveLoadResult
import com.sumizuri.app.data.live.LivePlaylist
import com.sumizuri.app.data.live.LiveSourceKind
import com.sumizuri.app.data.live.isTemporaryLiveFile
import com.sumizuri.app.data.live.redactLiveUrl
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.profile_save
import com.sumizuri.app.shared.resources.suki_live_add_list
import com.sumizuri.app.shared.resources.suki_live_channels_count
import com.sumizuri.app.shared.resources.suki_live_error_empty
import com.sumizuri.app.shared.resources.suki_live_error_read
import com.sumizuri.app.shared.resources.suki_live_error_save
import com.sumizuri.app.shared.resources.suki_live_error_too_large
import com.sumizuri.app.shared.resources.suki_live_error_url
import com.sumizuri.app.shared.resources.suki_live_file_selected
import com.sumizuri.app.shared.resources.suki_live_list_name
import com.sumizuri.app.shared.resources.suki_live_list_name_hint
import com.sumizuri.app.shared.resources.suki_live_loading
import com.sumizuri.app.shared.resources.suki_live_manage_lists
import com.sumizuri.app.shared.resources.suki_live_pick_file
import com.sumizuri.app.shared.resources.suki_live_refresh
import com.sumizuri.app.shared.resources.suki_live_remove
import com.sumizuri.app.shared.resources.suki_live_remove_confirm
import com.sumizuri.app.shared.resources.suki_live_rename
import com.sumizuri.app.shared.resources.suki_live_source_file
import com.sumizuri.app.shared.resources.suki_live_source_url
import com.sumizuri.app.shared.resources.suki_live_url_hint
import com.sumizuri.app.shared.resources.suki_live_url_label
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import com.sumizuri.app.ui.kit.input.PickedFile
import com.sumizuri.app.ui.kit.input.rememberFilesImporter
import com.sumizuri.app.ui.kit.page.AppSheet
import kotlinx.coroutines.launch
import okio.Path
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddLivePlaylistSheet(
    onAddUrl: suspend (String, String) -> LiveLoadResult,
    onAddFile: suspend (String, Path, String?, Boolean) -> LiveLoadResult,
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(LiveSourceKind.Url) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var pickedFile by remember { mutableStateOf<PickedFile?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    val scope = rememberCoroutineScope()
    val pickFile = rememberFilesImporter { files ->
        val file = files.firstOrNull() ?: return@rememberFilesImporter
        pickedFile = file
        if (name.isBlank() && !isTemporaryLiveFile(file.name)) {
            name = file.name.substringBeforeLast('.').ifBlank { file.name }
        }
        error = null
    }

    AppSheet(onDismiss = {
        if (!busy) onDismiss()
    }, spacing = 12.dp, title = stringResource(Res.string.suki_live_add_list)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = kind == LiveSourceKind.Url,
                onClick = { if (!busy) kind = LiveSourceKind.Url },
                label = { Text(stringResource(Res.string.suki_live_source_url)) },
            )
            FilterChip(
                selected = kind == LiveSourceKind.File,
                onClick = { if (!busy) kind = LiveSourceKind.File },
                label = { Text(stringResource(Res.string.suki_live_source_file)) },
            )
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(60) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            singleLine = true,
            label = { Text(stringResource(Res.string.suki_live_list_name)) },
            placeholder = { Text(stringResource(Res.string.suki_live_list_name_hint)) },
        )
        if (kind == LiveSourceKind.Url) {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                singleLine = true,
                label = { Text(stringResource(Res.string.suki_live_url_label)) },
                placeholder = { Text(stringResource(Res.string.suki_live_url_hint)) },
            )
        } else {
            OutlinedButton(onClick = pickFile, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp))
                Text(
                    pickedFile?.let { stringResource(Res.string.suki_live_file_selected, it.name) }
                        ?: stringResource(Res.string.suki_live_pick_file),
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        error?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (busy) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InkCircularProgressIndicator(Modifier.size(20.dp))
                Text(stringResource(Res.string.suki_live_loading), style = MaterialTheme.typography.bodySmall)
            }
        }
        val canSubmit = !busy && if (kind == LiveSourceKind.Url) url.isNotBlank() else pickedFile != null
        InkButton(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    val result = when (kind) {
                        LiveSourceKind.Url -> onAddUrl(name, url)

                        LiveSourceKind.File -> {
                            val file = pickedFile!!
                            onAddFile(name, file.path, file.name, file.temporary)
                        }
                    }
                    busy = false
                    // The picker copy is gone after a failed try, so the file is asked for again.
                    if (result !is LiveLoadResult.Ok && kind == LiveSourceKind.File &&
                        pickedFile?.temporary == true
                    ) {
                        pickedFile = null
                    }
                    when (result) {
                        is LiveLoadResult.Ok -> onDismiss()

                        LiveLoadResult.InvalidUrl -> error = Res.string.suki_live_error_url

                        LiveLoadResult.TooLarge -> error = Res.string.suki_live_error_too_large

                        LiveLoadResult.Empty -> error = Res.string.suki_live_error_empty

                        LiveLoadResult.ReadFailed, LiveLoadResult.PickFileRequired ->
                            error =
                                Res.string.suki_live_error_read

                        LiveLoadResult.SaveFailed -> error = Res.string.suki_live_error_save
                    }
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.suki_live_add_list))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageLivePlaylistsSheet(
    playlists: List<LivePlaylist>,
    onRefresh: suspend (String) -> LiveLoadResult,
    onReplaceFile: suspend (String, Path, Boolean) -> LiveLoadResult,
    onRename: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    var busyId by remember { mutableStateOf<String?>(null) }
    var pendingPickId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    var renaming by remember { mutableStateOf<LivePlaylist?>(null) }
    var removing by remember { mutableStateOf<LivePlaylist?>(null) }
    val scope = rememberCoroutineScope()
    val pickReplacement = rememberFilesImporter { files ->
        val targetId = pendingPickId
        pendingPickId = null
        val file = files.firstOrNull() ?: return@rememberFilesImporter
        if (targetId == null) return@rememberFilesImporter
        busyId = targetId
        error = null
        scope.launch {
            val res = onReplaceFile(targetId, file.path, file.temporary)
            busyId = null
            error = loadErrorResource(res)
        }
    }

    AppSheet(onDismiss = onDismiss, spacing = 10.dp, title = stringResource(Res.string.suki_live_manage_lists)) {
        error?.let {
            Text(
                stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        for (playlist in playlists) {
            val colors = MaterialTheme.colorScheme
            val sourceLabel = if (playlist.kind == LiveSourceKind.File && isTemporaryLiveFile(playlist.source)) {
                stringResource(Res.string.suki_live_source_file)
            } else {
                redactLiveUrl(playlist.source)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(Res.string.suki_live_channels_count, playlist.channelCount) + " · " +
                                sourceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (busyId == playlist.id) {
                        InkCircularProgressIndicator(Modifier.size(20.dp).padding(end = 8.dp))
                    } else {
                        IconButton(onClick = {
                            if (playlist.kind == LiveSourceKind.File && isTemporaryLiveFile(playlist.source)) {
                                pendingPickId = playlist.id
                                pickReplacement()
                                return@IconButton
                            }
                            busyId = playlist.id
                            error = null
                            scope.launch {
                                val res = onRefresh(playlist.id)
                                busyId = null
                                if (res == LiveLoadResult.PickFileRequired) {
                                    pendingPickId = playlist.id
                                    pickReplacement()
                                } else {
                                    error = loadErrorResource(res)
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Refresh, stringResource(Res.string.suki_live_refresh))
                        }
                    }
                    IconButton(onClick = { renaming = playlist }) {
                        Icon(Icons.Outlined.Edit, stringResource(Res.string.suki_live_rename))
                    }
                    IconButton(onClick = { removing = playlist }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            stringResource(Res.string.suki_live_remove),
                            tint = colors.error,
                        )
                    }
                }
            }
        }
        InkButton(onClick = onAddNew, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.suki_live_add_list))
        }
    }

    renaming?.let { target ->
        var draft by remember(target.id) { mutableStateOf(target.name) }
        AppDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(Res.string.suki_live_rename)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(60) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.suki_live_list_name)) },
                )
            },
            confirmButton = {
                InkButton(
                    onClick = {
                        onRename(target.id, draft)
                        renaming = null
                    },
                    enabled = draft.isNotBlank(),
                ) { Text(stringResource(Res.string.profile_save)) }
            },
            dismissButton = { CancelButton({ renaming = null }) },
        )
    }

    removing?.let { target ->
        AppDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(Res.string.suki_live_remove)) },
            text = { Text(stringResource(Res.string.suki_live_remove_confirm, target.name)) },
            confirmButton = {
                InkButton(onClick = {
                    onRemove(target.id)
                    removing = null
                }) { Text(stringResource(Res.string.suki_live_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { CancelButton({ removing = null }) },
        )
    }
}

private fun loadErrorResource(result: LiveLoadResult): StringResource? = when (result) {
    is LiveLoadResult.Ok -> null
    LiveLoadResult.InvalidUrl -> Res.string.suki_live_error_url
    LiveLoadResult.TooLarge -> Res.string.suki_live_error_too_large
    LiveLoadResult.Empty -> Res.string.suki_live_error_empty
    LiveLoadResult.ReadFailed, LiveLoadResult.PickFileRequired -> Res.string.suki_live_error_read
    LiveLoadResult.SaveFailed -> Res.string.suki_live_error_save
}
