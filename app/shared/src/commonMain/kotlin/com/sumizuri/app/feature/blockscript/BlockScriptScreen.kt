package com.sumizuri.app.feature.blockscript

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.BlockContext
import com.sumizuri.app.data.blocks.ProfileBlock
import com.sumizuri.app.data.blocks.ScriptResult
import com.sumizuri.app.data.blocks.SimpleBlock
import com.sumizuri.app.data.blocks.fit
import com.sumizuri.app.data.blocks.runScript
import com.sumizuri.app.data.blocks.simpleOf
import com.sumizuri.app.feature.browse.editor.CodeEditor
import com.sumizuri.app.feature.profile.blocks.BlockEnv
import com.sumizuri.app.feature.profile.blocks.BlockViewContent
import com.sumizuri.app.feature.profile.blocks.GridColumns
import com.sumizuri.app.feature.profile.blocks.GridGap
import com.sumizuri.app.feature.profile.blocks.heightOfRows
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_guide
import com.sumizuri.app.shared.resources.script_missing
import com.sumizuri.app.shared.resources.script_mode
import com.sumizuri.app.shared.resources.script_preview
import com.sumizuri.app.shared.resources.script_size_fit
import com.sumizuri.app.shared.resources.script_size_fixed
import com.sumizuri.app.shared.resources.script_tab_edit
import com.sumizuri.app.shared.resources.script_title
import com.sumizuri.app.shared.resources.simple_by_hand
import com.sumizuri.app.shared.resources.simple_mode
import com.sumizuri.app.shared.resources.simple_start_over
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.input.rememberCopyText
import com.sumizuri.app.ui.kit.page.AppSheet
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.page.PageTopBar
import com.sumizuri.app.ui.kit.setting.PartTabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

// Wide enough for the preview and the guide to sit beside the script.
private val WideBreakpoint = 840.dp

// How long typing has to pause before the preview runs the script again.
private const val PreviewAfterMs = 200L

// The page where a block is made: the simple editor, where it is put together with taps, or the script itself, with the block as the profile will show it, run again on every change. A block made by the simple editor opens there, and any other in its script, where a guide tells what a script can say and puts its pieces in with a tap. On a phone the preview sits over the editor and the guide opens as a sheet.
@Composable
internal fun BlockScriptScreen(
    text: TextFieldValue,
    block: ProfileBlock?,
    missing: Boolean,
    reading: BlockContext?,
    onText: (TextFieldValue) -> Unit,
    onSimple: (SimpleBlock) -> Unit,
    onInsert: (String) -> Unit,
    onFormat: () -> Unit,
    onBack: () -> Unit,
) {
    var guide by rememberSaveable { mutableStateOf(false) }
    val simple = remember(text.text) { simpleOf(text.text) }
    // Which editor shows, chosen once the block is there: the simple one for a block it made.
    var scripting by rememberSaveable(block?.id) { mutableStateOf(block != null && simpleOf(block.script) == null) }
    // Every state of the text is kept, so a step can be taken back, and the first mistake in it is looked for as it changes.
    val history = remember(block?.id) { EditHistory() }
    if (block != null) LaunchedEffect(text) { history.seen(text) }
    val problem = remember(text.text) { scriptProblem(text.text) }
    val copy = rememberCopyText()
    // On a phone the editor and the preview each take the whole screen, one at a time.
    var phoneTab by rememberSaveable { mutableStateOf(PhoneTab.Edit) }
    val result by produceState<ScriptResult?>(null, text.text, reading) {
        val context = reading ?: return@produceState
        delay(PreviewAfterMs)
        value = withContext(Dispatchers.Default) { runScript(text.text, context) }
    }
    // No page color of its own, so the ink of the theme behind every page shows here too.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideBreakpoint
        Column(Modifier.fillMaxSize()) {
            PageTopBar(stringResource(Res.string.script_title), onBack = onBack) {
                if (scripting &&
                    !wide
                ) {
                    IconButton(
                        { guide = true },
                    ) { Icon(Icons.AutoMirrored.Outlined.MenuBook, stringResource(Res.string.script_guide)) }
                }
            }
            if (missing) {
                Text(
                    stringResource(Res.string.script_missing),
                    Modifier.padding(PageGutter),
                    color = MaterialTheme.colorScheme.outline,
                )
                return@Column
            }
            if (block == null) return@Column
            ModeSwitch(scripting, Modifier.padding(horizontal = PageGutter).padding(bottom = 10.dp)) { scripting = it }
            val editor = @Composable { modifier: Modifier ->
                if (scripting) {
                    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ScriptToolbar(history, onUndo = {
                            history.undo()?.let(onText)
                        }, onRedo = { history.redo()?.let(onText) }, onFormat = onFormat, onCopy = { copy(text.text) })
                        CodeEditor(text, onText, Modifier.weight(1f).fillMaxWidth())
                        ScriptStatus(problem, onJump = { at -> onText(text.copy(selection = TextRange(at))) })
                    }
                } else {
                    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        if (simple != null) SimpleEditor(simple, onSimple) else StartOver { onSimple(SimpleBlock()) }
                    }
                }
            }
            if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = PageGutter).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    editor(Modifier.weight(1f).fillMaxHeight())
                    Column(
                        Modifier.width(380.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Preview(result, block, reading, folding = false)
                        if (scripting) ScriptGuide(onInsert)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = PageGutter).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PartTabs(PhoneTab.entries, phoneTab, {
                        stringResource(
                            if (it ==
                                PhoneTab.Edit
                            ) {
                                Res.string.script_tab_edit
                            } else {
                                Res.string.script_preview
                            },
                        )
                    }) { phoneTab = it }
                    when (phoneTab) {
                        PhoneTab.Edit -> editor(Modifier.weight(1f).fillMaxWidth())

                        PhoneTab.Preview -> Column(
                            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        ) {
                            Preview(result, block, reading, folding = false)
                        }
                    }
                }
            }
        }
        if (guide && !wide && scripting) {
            AppSheet({ guide = false }, title = stringResource(Res.string.script_guide)) {
                ScriptGuide {
                    onInsert(it)
                    guide = false
                }
            }
        }
    }
}

// The two sides of the page on a phone.
private enum class PhoneTab { Edit, Preview }

// The switch between the simple editor and the script.
@Composable
private fun ModeSwitch(scripting: Boolean, modifier: Modifier, onScripting: (Boolean) -> Unit) {
    PartTabs(listOf(false, true), scripting, {
        stringResource(if (it) Res.string.script_mode else Res.string.simple_mode)
    }, modifier) { onScripting(it) }
}

// What the simple editor says for a script written by hand, which it cannot show as choices: that starting over here replaces the script.
@Composable
private fun StartOver(onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.simple_by_hand),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InkOutlinedButton(onStart) { Text(stringResource(Res.string.simple_start_over)) }
    }
}

// The four columns of the page as faint marks, so the width of the block is easy to read against them.
@Composable
private fun PageColumns(cell: androidx.compose.ui.unit.Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(GridGap)) {
        repeat(GridColumns) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .width(
                        cell,
                    ).height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall),
            )
        }
    }
}

// How tall the preview of a block with no set height may grow before it scrolls.
private val FreePreviewHeight = 320.dp

// The block as the profile shows it, at its set height when it has one. On a phone it folds away to give the script more room.
@Composable
private fun Preview(result: ScriptResult?, block: ProfileBlock, reading: BlockContext?, folding: Boolean) {
    var open by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().then(
                if (folding) {
                    Modifier.clickable {
                        open = !open
                    }
                } else {
                    Modifier
                },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.script_preview),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            if (folding) Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        if (!open || result == null || reading == null) return@Column
        val env = BlockEnv(reading.stats, emptyList(), wide = false, onOpenTitle = {})
        // The block is drawn as wide and as tall as it is on the profile, on the four columns of the page, so what is seen here is what the profile shows.
        val (columns, rows) = result.view.fit().clamp(block.w, block.h)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cell = (maxWidth - GridGap * (GridColumns - 1)) / GridColumns
            val span = cell * columns + GridGap * (columns - 1)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PageColumns(cell)
                AppCard(
                    Modifier.width(span).then(
                        if (rows >
                            0
                        ) {
                            Modifier.height(heightOfRows(rows))
                        } else {
                            Modifier.heightIn(max = FreePreviewHeight)
                        },
                    ),
                ) {
                    Column(
                        Modifier.padding(14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (result.title.isNotBlank()) {
                            Text(
                                result.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        BlockViewContent(result.view, env, Modifier.fillMaxWidth())
                    }
                }
                Text(
                    if (rows >
                        0
                    ) {
                        stringResource(Res.string.script_size_fixed, columns, rows)
                    } else {
                        stringResource(Res.string.script_size_fit, columns)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
