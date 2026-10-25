package com.sumizuri.app.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.db.download.DownloadRow
import com.sumizuri.app.data.db.download.DownloadState
import com.sumizuri.app.data.db.download.progress
import com.sumizuri.app.data.db.download.status
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.download_cancel
import com.sumizuri.app.shared.resources.download_delete
import com.sumizuri.app.shared.resources.download_empty
import com.sumizuri.app.shared.resources.download_failed_section
import com.sumizuri.app.shared.resources.download_pages
import com.sumizuri.app.shared.resources.download_pause
import com.sumizuri.app.shared.resources.download_paused
import com.sumizuri.app.shared.resources.download_queue_section
import com.sumizuri.app.shared.resources.download_resume
import com.sumizuri.app.shared.resources.download_retry_all
import com.sumizuri.app.shared.resources.download_saved_section
import com.sumizuri.app.shared.resources.download_summary
import com.sumizuri.app.shared.resources.download_title
import com.sumizuri.app.shared.resources.download_waiting
import com.sumizuri.app.shared.resources.download_wifi_waiting
import com.sumizuri.app.shared.resources.settings_title
import com.sumizuri.app.shared.resources.try_again
import com.sumizuri.app.ui.kit.art.InkLinearProgressIndicator
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.page.PageList
import com.sumizuri.app.ui.kit.page.PageTopBar
import com.sumizuri.app.ui.kit.page.ReadableColumn
import com.sumizuri.app.ui.text.byteSizeText
import com.sumizuri.app.ui.text.numberedUnit
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The chapters saved to the device and those on the way: the queue with how far each one is, the ones that failed with why, and the saved ones by title with how much room they take. The queue can be paused, a chapter taken out, and the failed ones asked for again.
@Composable
fun DownloadsRoute(onBack: () -> Unit, onOptions: () -> Unit) {
    val downloads = LocalAppContainer.current.downloads
    // Asked for once, so drawing the page again does not start the query again.
    val rows by remember(downloads) { downloads.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val paused by downloads.paused.collectAsStateWithLifecycle(false)
    val waitingForWifi by downloads.waitingForWifi.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // The rows come in order, the one being saved first and the saved ones by title last, so each part keeps its order.
    val byState = remember(rows) { rows.groupBy { it.download.status } }
    val queue = byState[DownloadState.Running].orEmpty() + byState[DownloadState.Queued].orEmpty()
    val failed = byState[DownloadState.Failed].orEmpty()
    val saved = byState[DownloadState.Done].orEmpty()
    val remove: (DownloadRow) -> Unit = { row -> scope.launch { downloads.remove(listOf(row.download.chapterId)) } }
    ReadableColumn {
        Column(Modifier.fillMaxSize()) {
            PageTopBar(
                stringResource(Res.string.download_title),
                subtitle = stringResource(Res.string.download_summary, queue.size, saved.size, byteSizeText(saved.sumOf { it.download.bytes })),
                onBack = onBack,
            ) {
                if (failed.isNotEmpty()) IconButton({ scope.launch { downloads.retryFailed() } }) { Icon(Icons.Outlined.Refresh, stringResource(Res.string.download_retry_all)) }
                IconButton({ scope.launch { if (paused) downloads.resume() else downloads.pause() } }) {
                    Icon(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, stringResource(if (paused) Res.string.download_resume else Res.string.download_pause))
                }
                IconButton(onOptions) { Icon(Icons.Outlined.Tune, stringResource(Res.string.settings_title)) }
            }
            PageList(Modifier.fillMaxSize()) {
                if (rows.isEmpty()) {
                    item { Text(stringResource(Res.string.download_empty), Modifier.padding(vertical = 24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) }
                }
                if (queue.isNotEmpty()) {
                    item {
                        AppSectionLabel(
                            stringResource(Res.string.download_queue_section) + when {
                                paused -> " · " + stringResource(Res.string.download_paused)
                                waitingForWifi -> " · " + stringResource(Res.string.download_wifi_waiting)
                                else -> ""
                            },
                        )
                    }
                    items(queue, key = { "q${it.download.chapterId}" }) { row -> DownloadCard(row, onRemove = { remove(row) }) }
                }
                if (failed.isNotEmpty()) {
                    item { AppSectionLabel(Res.string.download_failed_section) }
                    items(failed, key = { "f${it.download.chapterId}" }) { row ->
                        DownloadCard(row, onRetry = { scope.launch { downloads.queue(row.download.titleId, listOf(row.download.chapterId)) } }, onRemove = { remove(row) })
                    }
                }
                if (saved.isNotEmpty()) {
                    item { AppSectionLabel(Res.string.download_saved_section) }
                    items(saved, key = { "s${it.download.chapterId}" }) { row -> DownloadCard(row, onRemove = { remove(row) }) }
                }
            }
        }
    }
}

// One chapter: its title and name, how far it is or why it failed, and the button that takes it out, which stops one on the way and deletes one that is saved or failed.
@Composable
private fun DownloadCard(row: DownloadRow, onRetry: (() -> Unit)? = null, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val download = row.download
    val state = download.status
    val waiting = state == DownloadState.Queued || state == DownloadState.Running
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                when (state) {
                    DownloadState.Done -> Icons.Filled.DownloadDone
                    DownloadState.Failed -> Icons.Outlined.ErrorOutline
                    else -> Icons.Outlined.Download
                },
                null,
                tint = when (state) {
                    DownloadState.Done -> colors.primary
                    DownloadState.Failed -> colors.error
                    else -> colors.onSurfaceVariant
                },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(row.titleName, style = MaterialTheme.typography.labelMedium, color = colors.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.chapterName ?: numberedUnit(Kind.Manga, row.number), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                when (state) {
                    DownloadState.Running -> {
                        InkLinearProgressIndicator(
                            progress = { download.progress },
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp),
                        )
                        Text("${download.saved} / ${download.pages}", style = MaterialTheme.typography.bodySmall, color = colors.outline)
                    }

                    DownloadState.Queued -> Text(stringResource(Res.string.download_waiting), style = MaterialTheme.typography.bodySmall, color = colors.outline)

                    DownloadState.Failed -> Text(download.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.error, maxLines = 3, overflow = TextOverflow.Ellipsis)

                    DownloadState.Done -> Text("${pluralStringResource(Res.plurals.download_pages, download.pages, download.pages)} · ${byteSizeText(download.bytes)}", style = MaterialTheme.typography.bodySmall, color = colors.outline)
                }
            }
            if (onRetry != null) IconButton(onRetry) { Icon(Icons.Outlined.Refresh, stringResource(Res.string.try_again)) }
            IconButton(onRemove) {
                Icon(if (waiting) Icons.Outlined.Close else Icons.Outlined.DeleteOutline, stringResource(if (waiting) Res.string.download_cancel else Res.string.download_delete))
            }
        }
    }
}
