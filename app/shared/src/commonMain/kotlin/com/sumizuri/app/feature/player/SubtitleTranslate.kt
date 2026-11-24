package com.sumizuri.app.feature.player

import com.sumizuri.app.feature.reader.translate.NovelTranslator
import com.sumizuri.app.playback.PlayerEngine
import com.sumizuri.app.playback.languageDisplayName
import com.sumizuri.app.source.model.SourceTrack
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.playback.PlayerTrack
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_player_translate_failed
import com.sumizuri.app.shared.resources.suki_player_translate_subs
import com.sumizuri.app.shared.resources.suki_player_translated_name
import com.sumizuri.app.shared.resources.suki_player_translating
import org.jetbrains.compose.resources.stringResource

// Where a subtitle is in the work of putting it into the language of the app.
enum class SubtitleWork { Idle, Working, Failed }

// Subtitles of the source put into the language of the app by the translator of the reader, the way a video site makes automatic ones. Only the words change: the times, the styles of the file and its layout stay, and marks inside a line, such as italics, are left out of the translated one. A file is made once for each subtitle and language, and kept while the device keeps its temporary files.
internal object SubtitleTranslate {
    val work = MutableStateFlow(SubtitleWork.Idle)
    private val http by lazy { HttpClient { install(HttpTimeout) { requestTimeoutMillis = 20_000 } } }
    private val folder: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sumizuri-subtitles"

    // Whether there is a subtitle file to translate, none of the source is in that language already, and the one made is not on the video yet.
    fun offered(files: List<SourceTrack>, tracks: List<PlayerTrack>, language: String, made: String): Boolean =
        files.isNotEmpty() && files.none { inLanguage(it, language) } && tracks.none { it.label == made }

    // Makes the subtitle in the language and turns it on: the one on now when it is a file of the source, else English, else the first.
    fun start(session: PlayerSession, engine: PlayerEngine, scope: CoroutineScope, language: String, name: String) {
        val video = session.state.value.current ?: return
        if (work.value == SubtitleWork.Working) return
        val onNow = engine.state.value.let { now -> now.subtitles.firstOrNull { it.id == now.subtitle } }
        val files = video.subtitles
        val from = files.firstOrNull { onNow != null && it.label == onNow.label } ?: files.firstOrNull { inLanguage(it, "en") }
            ?: files.firstOrNull() ?: return
        work.value = SubtitleWork.Working
        scope.launch {
            val path = runCatching { make(from, video.headers, language) }.getOrNull()
            if (path == null) {
                work.value = SubtitleWork.Failed
                return@launch
            }
            work.value = SubtitleWork.Idle
            session.loadSubtitle(path.toString(), name)
        }
    }

    private suspend fun make(track: SourceTrack, headers: Map<String, String>, language: String): Path? = withContext(Dispatchers.Default) {
        val base = "${(track.url + language).hashCode().toUInt().toString(16)}-$language"
        Kinds.map { folder / "$base.$it" }.firstOrNull { FileSystem.SYSTEM.exists(it) }?.let { return@withContext it }
        val answer = http.get(track.url) { headers { headers.forEach { (key, value) -> append(key, value) } } }
        if (!answer.status.isSuccess()) return@withContext null
        val text = answer.bodyAsText()
        val translated = NovelTranslator().use { translator -> translateFile(text) { lines -> translator.translateLines(lines, language) } }
            ?: return@withContext null
        FileSystem.SYSTEM.createDirectories(folder)
        // The name the file ends with is how the players tell its kind, so it follows what the file holds.
        val path = folder / "$base.${kindOfText(translated) ?: kindOf(track.url)}"
        FileSystem.SYSTEM.write(path) { writeUtf8(translated) }
        path
    }

    private fun inLanguage(track: SourceTrack, language: String): Boolean {
        val code = track.language?.lowercase().orEmpty()
        if (code == language || code.startsWith("$language-") || code.startsWith("${language}_")) return true
        val name = languageDisplayName(language)?.lowercase() ?: return false
        return track.label.lowercase().contains(name)
    }

    private fun kindOf(url: String): String =
        url.substringBefore('?').substringAfterLast('.', "").lowercase().takeIf { it in Kinds } ?: "srt"

    private fun kindOfText(text: String): String? = when {
        text.trimStart().startsWith("WEBVTT") -> "vtt"
        "[Script Info]" in text -> "ass"
        "-->" in text -> "srt"
        else -> null
    }

    private val Kinds = setOf("srt", "vtt", "ass", "ssa")
}

// What the line that translates subtitles says: what it does, that it is at work, or that it failed and can be tried again.
@Composable
internal fun translateTitle(language: String): String {
    val work by SubtitleTranslate.work.collectAsStateWithLifecycle()
    return when (work) {
        SubtitleWork.Working -> stringResource(Res.string.suki_player_translating)
        SubtitleWork.Failed -> stringResource(Res.string.suki_player_translate_failed)
        SubtitleWork.Idle -> stringResource(Res.string.suki_player_translate_subs, languageName(language))
    }
}

// The name of the subtitle made, such as "Español (automático)".
@Composable
internal fun translatedName(language: String): String = stringResource(Res.string.suki_player_translated_name, languageName(language))

private fun languageName(code: String) = (languageDisplayName(code) ?: code).replaceFirstChar { it.titlecase() }

// The marks of styles inside the words of a line, such as <i> or {\an8}, left out of what is translated.
private val Marks = Regex("<[^>]*>|\\{[^}]*\\}")

// The text of a subtitle file with the words of every line put through translate, which answers in the same order, or null when it gave nothing. SRT and VTT keep their words under the line with the times; ASS keeps them after the ninth comma of each Dialogue line. A line said over two rows goes as one, so it reads as one sentence.
internal suspend fun translateFile(text: String, translate: suspend (List<String>) -> List<String>?): String? {
    val lines: MutableList<String?> = text.replace("\r\n", "\n").replace('\r', '\n').split('\n').toMutableList()
    val cues = mutableListOf<IntRange>()
    val dialogues = mutableListOf<Pair<Int, Int>>()
    var at = 0
    while (at < lines.size) {
        val line = lines[at].orEmpty()
        if (line.startsWith("Dialogue:")) {
            nthComma(line, 9)?.let { dialogues += at to it + 1 }
        } else if ("-->" in line) {
            var end = at + 1
            while (end < lines.size && lines[end].orEmpty().isNotBlank()) end++
            if (end > at + 1) cues += (at + 1) until end
            at = end
            continue
        }
        at++
    }
    val words = cues.map { range -> range.joinToString(" ") { lines[it].orEmpty().trim() } } +
        dialogues.map { (row, from) -> lines[row].orEmpty().substring(from).replace("\\N", " ").replace("\\n", " ") }
    val plain = words.map { it.replace(Marks, "").trim() }
    val done = translate(plain) ?: return null
    cues.forEachIndexed { index, range ->
        if (plain[index].isEmpty()) return@forEachIndexed
        lines[range.first] = done[index]
        for (row in range.drop(1)) lines[row] = null
    }
    dialogues.forEachIndexed { index, (row, from) ->
        val words = plain[cues.size + index]
        if (words.isNotEmpty()) lines[row] = lines[row].orEmpty().substring(0, from) + done[cues.size + index]
    }
    return lines.filterNotNull().joinToString("\n")
}

private fun nthComma(line: String, count: Int): Int? {
    var seen = 0
    line.forEachIndexed { index, char ->
        if (char == ',' && ++seen == count) return index
    }
    return null
}
