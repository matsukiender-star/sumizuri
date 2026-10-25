package com.sumizuri.app.feature.browse.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.core.log.LogEntry
import com.sumizuri.app.core.log.LogLevel
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.clear
import com.sumizuri.app.shared.resources.ext_edit_console
import com.sumizuri.app.shared.resources.ext_edit_console_empty
import org.jetbrains.compose.resources.stringResource

// What the draft said while it was tried: its console lines, its traces and its requests, each with the time since the first line, colored by how serious it is. It follows the newest line, the lines can be selected and copied, and a tap on its name folds it down to give the results more room.
@Composable
internal fun EditorConsole(
    lines: List<LogEntry>,
    onClear: () -> Unit,
    open: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val list = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) list.scrollToItem(lines.size - 1) }
    Surface(modifier, color = scheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.ext_edit_console) +
                        if (open ||
                            lines.isEmpty()
                        ) {
                            ""
                        } else {
                            " · ${lines.size}"
                        },
                    Modifier.weight(1f).clickable(onClick = onToggle).padding(vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (open) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                    null,
                    Modifier.clickable(onClick = onToggle).padding(8.dp).size(20.dp),
                    tint = scheme.outline,
                )
                TextButton(onClear, enabled = lines.isNotEmpty()) { Text(stringResource(Res.string.clear)) }
            }
            // Folded down, only the name shows.
            if (!open) return@Column
            if (lines.isEmpty()) {
                Text(
                    stringResource(Res.string.ext_edit_console_empty),
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.outline,
                )
            } else {
                val first = lines.first().time
                SelectionContainer {
                    LazyColumn(state = list, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                        items(lines) { line ->
                            val color = when (line.level) {
                                LogLevel.Error -> scheme.error
                                LogLevel.Warn -> scheme.tertiary
                                LogLevel.Debug -> scheme.outline
                                LogLevel.Info -> scheme.onSurface
                            }
                            Text(
                                "${stamp(line.time - first)}  ${line.message}",
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

// Seconds with their thousandths, such as +1.204.
private fun stamp(ms: Long): String = "+${ms / 1000}.${(ms % 1000).toString().padStart(3, '0')}"
