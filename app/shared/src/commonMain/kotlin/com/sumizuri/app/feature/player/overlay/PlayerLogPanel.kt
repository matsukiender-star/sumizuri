package com.sumizuri.app.feature.player.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.feature.player.PlaybackLog
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_log_copied
import com.sumizuri.app.shared.resources.player_log_copy
import com.sumizuri.app.shared.resources.player_log_empty
import com.sumizuri.app.ui.kit.input.rememberCopyText
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// The strip over the top of the video that says what the player is doing. It shows the last line, opens to every line on a tap, and copies the whole log, for finding out why something does not play.
@Composable
internal fun PlayerLogPanel(log: PlaybackLog, heading: String, modifier: Modifier = Modifier) {
    val entries by log.entries.collectAsState()
    var open by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val copy = rememberCopyText()
    val list = rememberLazyListState()
    val error = MaterialTheme.colorScheme.error
    val latest = entries.lastOrNull()
    // New lines come at the bottom, so the list follows them.
    LaunchedEffect(entries.size, open) { if (open && entries.isNotEmpty()) list.scrollToItem(entries.lastIndex) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.72f))) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    open = !open
                }.padding(
                    start = 10.dp,
                    top = 2.dp,
                    bottom = 2.dp,
                    end = 2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (log.hasProblem) Icons.Outlined.ErrorOutline else Icons.Outlined.Terminal,
                null,
                Modifier.size(14.dp),
                tint = if (log.hasProblem) error else Color.White.copy(alpha = 0.6f),
            )
            Text(
                latest
                    ?.text
                    ?.lineSequence()
                    ?.first()
                    ?.trim() ?: stringResource(Res.string.player_log_empty),
                Modifier.weight(1f).padding(start = 8.dp),
                color = if (latest?.problem == true) error else Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton({
                copy(log.toText(heading))
                copied = true
            }, Modifier.size(32.dp), enabled = entries.isNotEmpty()) {
                Icon(
                    if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    stringResource(if (copied) Res.string.player_log_copied else Res.string.player_log_copy),
                    Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.7f),
                )
            }
            Icon(
                if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                null,
                Modifier.padding(end = 6.dp).size(18.dp),
                tint = Color.White.copy(alpha = 0.55f),
            )
        }
        if (open) {
            SelectionContainer {
                LazyColumn(
                    Modifier.heightIn(max = 190.dp).padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                    state = list,
                ) {
                    items(entries) { entry ->
                        Text(
                            entry.line,
                            Modifier.padding(bottom = 2.dp),
                            color = if (entry.problem) error else Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
