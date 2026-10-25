package com.sumizuri.app.feature.browse.extensions

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_file_empty
import com.sumizuri.app.shared.resources.ext_file_unreadable
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.repo.EngineKind
import com.sumizuri.app.source.repo.InstalledSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.getString

// Installs a source from a file the person has, an extension in the source format. The file is started once to check it runs. Such a file says nothing about itself, so its name starts from the file name and the person fills in the rest. It is then kept under an id of its own, and the same file added again takes the place of the first.
class SourceFileImporter(private val manager: SourceManager, private val sources: SourceRepository) {
    suspend fun read(path: Path): ImportSourceState = withContext(Dispatchers.IO) {
        val text = try {
            FileSystem.SYSTEM.read(path) { readUtf8() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return@withContext ImportSourceState(problem = getString(Res.string.ext_file_unreadable))
        }
        if (text.isBlank()) return@withContext ImportSourceState(problem = getString(Res.string.ext_file_empty))
        val probe = InstalledSource(
            id = "probe:${nowMillis()}", repoUrl = null, name = path.name, lang = "", kind = Kind.Manga,
            engineKind = EngineKind.Current, version = 1, iconUrl = "", baseUrl = "", code = text,
        )
        val opened = manager.open(probe)
        manager.close(probe.id)
        when (opened) {
            is SourceResult.Err -> ImportSourceState(problem = opened.error.message)

            is SourceResult.Ok -> {
                ImportSourceState(
                    name = path.name
                        .substringBeforeLast('.')
                        .takeIf { it.lowercase() != "index" }
                        .orEmpty(),
                    code = text,
                )
            }
        }
    }

    // Keeps the source. A source added from a file before under the same name is replaced and its version goes up, so the engine starts the new code.
    suspend fun install(draft: ImportSourceState) {
        val id = "local:" + draft.name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "source" }
        val before = sources.find(id)
        sources.save(
            InstalledSource(
                id = id, repoUrl = null, name = draft.name.trim().ifEmpty { id }, lang = draft.lang.ifBlank { "multi" }, kind = draft.kind,
                engineKind = EngineKind.Current, version = (before?.version ?: 0) + 1, iconUrl = draft.iconUrl, baseUrl = draft.baseUrl, code = draft.code,
            ),
        )
    }
}
