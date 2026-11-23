package com.sumizuri.app.data.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_notice_open
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.card.AppCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// A notice of the system waits this long for a click, since a notification may never be closed by itself.
private const val WaitMs = 60_000L

// How long the notice in the window stays before it goes by itself.
private const val ShownMs = 15_000L

internal val isLinux = System
    .getProperty("os.name")
    .orEmpty()
    .lowercase()
    .contains("linux")

// Shows a notice through notify-send, so it looks like the rest of the desktop instead of the plain balloon of Java. Returns false when the system has no notify-send, and the window shows the notice itself.
internal suspend fun showLinuxNotice(head: String, text: String, onClick: () -> Unit): Boolean {
    val program = notifySend() ?: return false
    val open = runCatching { getString(Res.string.suki_notice_open) }.getOrNull()
    val icon = withContext(Dispatchers.IO) { runCatching { noticeIcon() }.getOrNull() }
    val clicked = withContext(Dispatchers.IO) { runNotice(program, head, text, icon, open) } ?: return false
    if (clicked) {
        onClick()
        DesktopFront.bring()
    }
    return true
}

// Returns whether the notice was clicked, or null when no notice could be shown at all.
private fun runNotice(program: File, head: String, text: String, icon: File?, open: String?): Boolean? {
    // The body is read as markup, so the marks of the text itself are written out.
    val body = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    if (open != null) {
        // An old notify-send has no actions and fails on the options it does not know, so the notice is sent again without one.
        val clicked =
            send(program, head, body, icon, listOf("--wait", "--expire-time=$WaitMs", "--action=default=$open"))
        if (clicked != null) return clicked
    }
    return send(program, head, body, icon, emptyList())
}

// Runs notify-send with the arguments as a list, never through a shell, since the words come from sources outside the app.
private fun send(program: File, head: String, body: String, icon: File?, extra: List<String>): Boolean? {
    val command =
        listOf(program.absolutePath, "--app-name=Sumizuri", "--icon=${icon?.absolutePath ?: "sumizuri"}") + extra +
            listOf("--", head, body)
    // The AppImage brings libraries of its own, which the notice of the system must not take.
    val process =
        runCatching { ProcessBuilder(command).apply { environment().remove("LD_LIBRARY_PATH") }.start() }.getOrNull()
            ?: return null
    val said = StringBuilder()
    val reader = thread(isDaemon = true, name = "sumizuri-notice") {
        runCatching { process.inputStream.bufferedReader().forEachLine { said.appendLine(it) } }
    }
    // A notice that waits for a click ends by itself, and is stopped when the minute runs out.
    val limit = if ("--wait" in extra) WaitMs else 5_000L
    reader.join(limit)
    val timedOut = reader.isAlive
    if (timedOut) process.destroy()
    if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
    if (reader.isAlive) reader.join(1_000L)
    // The notice was on screen for its minute, so it was shown even if nobody clicked it.
    if (timedOut) return false
    val code = runCatching { process.exitValue() }.getOrNull() ?: return null
    if (code != 0) {
        AppLog.warn("updates", "the notice could not be shown, notify-send ended with $code")
        return null
    }
    // A click writes the key of the action.
    return said.isNotBlank()
}

// The notify-send of the system, looked for in the folders of the PATH without a shell.
private fun notifySend(): File? = System
    .getenv("PATH")
    .orEmpty()
    .split(File.pathSeparator)
    .filter { it.isNotBlank() }
    .map { File(it, "notify-send") }
    .firstOrNull { it.isFile && it.canExecute() }

private var iconFile: File? = null

// The icon of the app in a file of its own, since notify-send takes a path.
private suspend fun noticeIcon(): File {
    iconFile?.takeIf { it.isFile }?.let { return it }
    val file = File.createTempFile("sumizuri-notice", ".png").apply { deleteOnExit() }
    file.writeBytes(Res.readBytes("drawable/app_icon.png"))
    iconFile = file
    return file
}

// A notice the window shows itself when the system has no notify-send, since Linux never gets the plain balloon of Java.
object LinuxNotice {
    // The notice on show, if any.
    val shown = MutableStateFlow<Shown?>(null)

    fun show(head: String, text: String, onClick: () -> Unit) {
        shown.value = Shown(head, text, onClick)
    }

    fun dismiss() {
        shown.value = null
    }

    class Shown(val head: String, val text: String, val onClick: () -> Unit)
}

// The notice in the window, which does what a click on the notice of the system does.
@Composable
fun LinuxNoticeHost(modifier: Modifier = Modifier) {
    val notice by LinuxNotice.shown.collectAsState()
    LaunchedEffect(notice) {
        if (notice == null) return@LaunchedEffect
        delay(ShownMs)
        LinuxNotice.dismiss()
    }
    AnimatedVisibility(
        notice != null,
        modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val shown = notice ?: return@AnimatedVisibility
        val open = {
            shown.onClick()
            DesktopFront.bring()
            LinuxNotice.dismiss()
        }
        AppCard(Modifier.widthIn(max = 520.dp), onClick = open) {
            Row(
                Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        shown.head,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (shown.text.isNotBlank()) {
                        Text(
                            shown.text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                InkButton({ open() }) { Text(stringResource(Res.string.suki_notice_open)) }
                IconButton(LinuxNotice::dismiss) { Icon(Icons.Outlined.Close, null) }
            }
        }
    }
}
