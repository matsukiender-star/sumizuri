package com.sumizuri.app.feature.browse.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.core.log.LogEntry
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_default_name
import com.sumizuri.app.shared.resources.ext_save_failed
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.model.CommentSort
import com.sumizuri.app.source.model.SourceCatalog
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.EngineKind
import com.sumizuri.app.source.repo.InstalledSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

// The label the draft logs under, so its lines can be told from those of the sources the app uses.
private const val Label = "editor"

// The most lines of the console kept on screen.
private const val ConsoleLines = 300

// Writes an extension and tries it. The draft is started apart from the sources the app uses, as a test the script knows about, and started again only when the code or the details change. Saving keeps it as a source of the person's own: a source added by hand is replaced in place, and one from a repo is kept as a copy beside it.
class ExtensionEditorViewModel(
    private val sourceId: String?,
    private val sources: SourceRepository,
    private val manager: SourceManager,
) : ViewModel() {
    private val mutable = MutableStateFlow(ExtensionEditorState())
    val state: StateFlow<ExtensionEditorState> = mutable

    // What the draft said since the editor opened or the console was cleared: the lines of the script and its requests under the label, and how each call went under the name the code gives itself.
    private val since = MutableStateFlow(nowMillis())
    private val engineName = MutableStateFlow<String?>(null)
    val console: StateFlow<List<LogEntry>> = combine(AppLog.entries, engineName, since) { entries, name, from ->
        entries.filter { it.time >= from && (it.tag == Label || (name != null && it.tag == name)) }.takeLast(ConsoleLines)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The draft that is running for the tests, and what it was started from.
    private var running: Source? = null
    private var runningFrom: InstalledSource? = null
    private val starting = Mutex()
    private val tester = ExtensionTester(viewModelScope, mutable, ::draftSource, Label)

    // What the opened source had beside its code, which a copy keeps, such as the hosts the source may reach. The id is where saving goes, once it is a source of the person's own.
    private var extra = ""
    private var localId: String? = null
    private var loadJob: Job? = null

    // Reads the source to edit, or starts from the template, each time the page opens. The view models of a profile are kept while it is open, so leaving the page is what lets go of the draft.
    fun onEnter() {
        if (mutable.value.loaded || loadJob?.isActive == true) return
        since.value = nowMillis()
        loadJob = viewModelScope.launch {
            val original = sources.find(sourceId)
            mutable.value = if (original != null) {
                extra = original.extra
                localId = original.id.takeIf { original.repoUrl == null }
                ExtensionEditorState(
                    loaded = true, sourceId = original.id, fromRepo = original.repoUrl != null, code = original.code,
                    name = original.name, lang = original.lang, kind = original.kind, baseUrl = original.baseUrl, iconUrl = original.iconUrl,
                )
            } else {
                extra = ""
                localId = null
                ExtensionEditorState(loaded = true, code = ExtensionTemplate, name = getString(Res.string.ext_default_name), lang = "en", baseUrl = "https://library.example.com")
            }
        }
    }

    // Stops what runs and closes the draft when the page is left, so the next visit starts from what is saved.
    fun onLeave() {
        loadJob?.cancel()
        tester.stop()
        running?.close()
        running = null
        runningFrom = null
        engineName.value = null
        mutable.value = ExtensionEditorState()
    }

    fun onCode(text: String) {
        if (text != mutable.value.code) edit { it.copy(code = text) }
    }
    fun onName(text: String) = edit { it.copy(name = text) }
    fun onLang(text: String) = edit { it.copy(lang = text.trim()) }
    fun onKind(kind: Kind) = edit { it.copy(kind = kind) }
    fun onBaseUrl(text: String) = edit { it.copy(baseUrl = text.trim()) }
    fun onIconUrl(text: String) = edit { it.copy(iconUrl = text.trim()) }

    private fun edit(change: (ExtensionEditorState) -> ExtensionEditorState) = mutable.update { change(it).copy(dirty = true, problem = null) }

    // Starts the draft and copies what the code says about itself into the details: its name, language, site and icon.
    fun onFillFromCode() = tester.custom(TestStep.Start) { source ->
        val info = source.info
        mutable.update { now ->
            now.copy(
                name = info.name.takeIf { it.isNotBlank() } ?: now.name,
                lang = info.lang?.takeIf { it.isNotBlank() } ?: now.lang,
                baseUrl = info.baseUrl?.takeIf { it.isNotBlank() } ?: now.baseUrl,
                iconUrl = info.iconUrl?.takeIf { it.isNotBlank() } ?: now.iconUrl,
                dirty = true,
            )
        }
    }

    // The tests, which the tester runs on the draft as the code is now.
    fun onInfo() = tester.info()
    fun onPopular() = tester.popular()
    fun onLatest() = tester.latest()
    fun onSearch(query: String) = tester.search(query)
    fun onNextPage() = tester.nextPage()
    fun onOpenEntry(entry: SourceEntry) = tester.open(entry)
    fun onOpenChapter(chapter: SourceChapter) = tester.open(chapter)
    fun onComments(sort: CommentSort) = tester.comments(sort)
    fun onChapterComments(sort: CommentSort) = tester.chapterComments(sort)
    fun onFilters() = tester.filters()
    fun onCatalogs() = tester.catalogs()
    fun onOpenCatalog(catalog: SourceCatalog) = tester.catalog(catalog)
    fun onSettings() = tester.settings()
    fun onRunAll() = tester.runAll()
    fun onStopTest() = tester.stop()
    fun onTestBack() = tester.back()
    fun onShowView(view: TestView) = tester.show(view)

    // Hides the lines so far, and leaves the log of the app as it is.
    fun onClearConsole() {
        since.value = nowMillis() + 1
    }

    // The draft as a source, started again when the code or the details changed since the last step. A save and a test that ask at the same moment get the same one.
    private suspend fun draftSource(): Source = starting.withLock {
        val draft = draft()
        val current = running
        if (current != null && runningFrom == draft) return@withLock current
        current?.close()
        running = null
        when (val opened = manager.openTest(draft, Label)) {
            is SourceResult.Err -> throw IllegalStateException(opened.error.message)

            is SourceResult.Ok -> opened.value.also {
                running = it
                runningFrom = draft
                engineName.value = it.info.name
            }
        }
    }

    private fun draft(): InstalledSource {
        val now = mutable.value
        return InstalledSource(
            id = localId ?: sourceId ?: "draft", repoUrl = null, name = now.name, lang = now.lang, kind = now.kind, engineKind = EngineKind.Current,
            version = 0, iconUrl = now.iconUrl, baseUrl = now.baseUrl, code = now.code, extra = extra,
        )
    }

    // Keeps the draft as a source once it starts. A source added by hand is replaced and its version goes up, so the app starts the new code. One from a repo is kept as a copy of the person's own, and later saves go to that copy.
    fun onSave() {
        if (!mutable.value.canSave) return
        mutable.update { it.copy(saving = true, problem = null) }
        viewModelScope.launch {
            try {
                draftSource()
                val now = mutable.value
                val installed = sources.installed.first()
                val id = localId ?: freeId(now.name, installed)
                val before = installed.firstOrNull { it.id == id }
                sources.save(
                    InstalledSource(
                        id = id, repoUrl = null, name = now.name.trim(), lang = now.lang.ifBlank { "multi" }, kind = now.kind, engineKind = EngineKind.Current,
                        version = (before?.version ?: 0) + 1, iconUrl = now.iconUrl, baseUrl = now.baseUrl, code = now.code, extra = extra,
                        enabled = before?.enabled ?: true, lastUsedAt = before?.lastUsedAt ?: 0,
                    ),
                )
                localId = id
                AppLog.info(Label, "saved as $id")
                mutable.update { it.copy(saving = false, dirty = false, savedAs = id) }
                // The page was left while it saved, so the draft it started for the check is not kept.
                if (!mutable.value.loaded) onLeave()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutable.update { it.copy(saving = false, problem = error.message ?: getString(Res.string.ext_save_failed)) }
            }
        }
    }

    override fun onCleared() = onLeave()
}

// An id for a new source of the person's own, from its name, with a number after it when another source has it already.
private fun freeId(name: String, installed: List<InstalledSource>): String {
    val base = "local:" + name
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifEmpty { "source" }
    val taken = installed.map { it.id }.toSet()
    if (base !in taken) return base
    return generateSequence(2) { it + 1 }.map { "$base-$it" }.first { it !in taken }
}
