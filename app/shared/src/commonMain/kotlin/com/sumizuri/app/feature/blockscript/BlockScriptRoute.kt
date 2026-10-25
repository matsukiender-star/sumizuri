package com.sumizuri.app.feature.blockscript

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.blocks.BlockContext
import com.sumizuri.app.data.blocks.ProfileBlock
import com.sumizuri.app.data.blocks.ProfileBlocksRepository
import com.sumizuri.app.data.blocks.SimpleBlock
import com.sumizuri.app.data.blocks.script
import com.sumizuri.app.data.library.StatisticsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

// How long typing has to pause before the script is saved.
private const val SaveAfterMs = 500L

private val Pretty = Json { prettyPrint = true }

// The editor of the script of one block. The script is saved a moment after typing stops, and once more when the page is left, in order, and a save that is under way finishes even then.
class BlockScriptViewModel(
    private val blockId: String,
    private val blocks: ProfileBlocksRepository,
    statistics: StatisticsRepository,
) : ViewModel() {
    val reading: StateFlow<BlockContext?> = statistics.reading.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    var block by mutableStateOf<ProfileBlock?>(null)
        private set
    var text by mutableStateOf(TextFieldValue(""))
        private set

    // True when the block is not there, such as one removed on another screen.
    var missing by mutableStateOf(false)
        private set

    private val saving = Mutex()
    private var pending: Job? = null
    private var saved = ""

    init {
        viewModelScope.launch {
            val found = blocks.layout
                .first()
                .blocks
                .firstOrNull { it.id == blockId }
            if (found == null) {
                missing = true
            } else {
                block = found
                saved = found.script
                text = TextFieldValue(found.script)
            }
        }
    }

    fun onText(value: TextFieldValue) {
        val changed = value.text != text.text
        text = value
        if (!changed) return
        pending?.cancel()
        pending = viewModelScope.launch {
            delay(SaveAfterMs)
            save()
        }
    }

    // Puts a piece of script where the cursor is, in place of what is chosen.
    fun onInsert(piece: String) {
        val now = text
        val start = minOf(now.selection.start, now.selection.end)
        val end = maxOf(now.selection.start, now.selection.end)
        onText(TextFieldValue(now.text.replaceRange(start, end, piece), TextRange(start + piece.length)))
    }

    // A change made in the simple editor, which writes the script again.
    fun onSimple(block: SimpleBlock) {
        val script = block.script()
        onText(TextFieldValue(script, TextRange(script.length)))
    }

    // Lays the script out with one key to a line, when it is JSON that can be read.
    fun onFormat() {
        val tidy =
            runCatching {
                Pretty.encodeToString(
                    JsonElement.serializer(),
                    Json.parseToJsonElement(text.text),
                )
            }.getOrNull()
                ?: return
        onText(TextFieldValue(tidy, TextRange(tidy.length)))
    }

    fun save() {
        val script = text.text
        if (script == saved || block == null) return
        saved = script
        // The save finishes even when the screen closes right after.
        viewModelScope.launch {
            withContext(NonCancellable) { saving.withLock { blocks.updateBlock(blockId) { it.copy(script = script) } } }
        }
    }
}

@Composable
fun BlockScriptRoute(blockId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel =
        viewModel(
            key = "block_script_$blockId",
        ) { BlockScriptViewModel(blockId, container.blocks, container.statistics) }
    val reading by viewModel.reading.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) { onDispose { viewModel.save() } }
    BlockScriptScreen(
        text = viewModel.text,
        block = viewModel.block,
        missing = viewModel.missing,
        reading = reading,
        onText = viewModel::onText,
        onSimple = viewModel::onSimple,
        onInsert = viewModel::onInsert,
        onFormat = viewModel::onFormat,
        onBack = onBack,
    )
}
