package com.sumizuri.app.feature.blockscript

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_copy
import com.sumizuri.app.shared.resources.script_format
import com.sumizuri.app.shared.resources.script_problem_at
import com.sumizuri.app.shared.resources.script_redo
import com.sumizuri.app.shared.resources.script_undo
import com.sumizuri.app.shared.resources.script_valid
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

// Typing within this long of the last keystroke is one step of the history, so undo takes back a word and not a letter.
private const val BurstMs = 700L

// The most steps the history keeps.
private const val MostSteps = 100

// The steps of an edit, to go back and forth over. It sees every state the text takes, whoever changed it, and keeps them in order.
internal class EditHistory {
    private val steps = mutableListOf<TextFieldValue>()
    private var at = -1
    private var lastAt = 0L
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun seen(value: TextFieldValue) {
        if (at >= 0 && steps[at].text == value.text) {
            steps[at] = value
            return
        }
        while (steps.size - 1 > at) steps.removeAt(steps.lastIndex)
        val now = nowMillis()
        if (at >= 0 && now - lastAt < BurstMs) {
            steps[at] = value
        } else {
            steps += value
            at++
        }
        if (steps.size > MostSteps) {
            steps.removeAt(0)
            at--
        }
        lastAt = now
        update()
    }

    fun undo(): TextFieldValue? {
        if (at <= 0) return null
        at--
        update()
        return steps[at]
    }

    fun redo(): TextFieldValue? {
        if (at >= steps.size - 1) return null
        at++
        update()
        return steps[at]
    }

    private fun update() {
        canUndo = at > 0
        canRedo = at < steps.size - 1
    }
}

// Where a script is not valid JSON, and what is wrong there.
internal class ScriptProblem(val offset: Int, val line: Int, val column: Int, val message: String)

// Looks for the first mistake in the text of a script, as the place it is at and what is wrong, or null when the text is valid JSON.
internal fun scriptProblem(text: String): ScriptProblem? {
    if (text.isBlank()) return null
    return try {
        Json.parseToJsonElement(text)
        null
    } catch (error: Exception) {
        val said = error.message.orEmpty()
        val offset = Regex("offset (\\d+)")
            .find(said)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?.coerceIn(0, text.length) ?: 0
        val before = text.substring(0, offset)
        val line = before.count { it == '\n' } + 1
        val column = offset - (before.lastIndexOf('\n') + 1) + 1
        ScriptProblem(offset, line, column, said.substringAfter(": ", said).substringBefore('\n').take(140))
    }
}

// The things done to the text of a script: take a step back or forward, set it in order, and copy it all.
@Composable
internal fun ScriptToolbar(
    history: EditHistory,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFormat: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onUndo, Modifier.size(40.dp), enabled = history.canUndo) {
            Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(Res.string.script_undo))
        }
        IconButton(onRedo, Modifier.size(40.dp), enabled = history.canRedo) {
            Icon(Icons.AutoMirrored.Outlined.Redo, stringResource(Res.string.script_redo))
        }
        IconButton(onFormat, Modifier.size(40.dp)) {
            Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, stringResource(Res.string.script_format))
        }
        IconButton(
            onCopy,
            Modifier.size(40.dp),
        ) { Icon(Icons.Outlined.ContentCopy, stringResource(Res.string.script_copy)) }
    }
}

// The line under the script: that it is valid, or where the first mistake is. A tap on a mistake moves the cursor to it.
@Composable
internal fun ScriptStatus(problem: ScriptProblem?, onJump: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    if (problem == null) {
        Row(
            modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = scheme.primary)
            Text(
                stringResource(Res.string.script_valid),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        return
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(scheme.errorContainer, MaterialTheme.shapes.medium)
            .clickable { onJump(problem.offset) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(18.dp), tint = scheme.onErrorContainer)
        Text(
            stringResource(Res.string.script_problem_at, problem.line, problem.column, problem.message),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onErrorContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
