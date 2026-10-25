package com.sumizuri.app.feature.browse.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.sources.RepoMigration
import com.sumizuri.app.data.sources.RepoRepository
import com.sumizuri.app.data.sources.SourceUpdate
import com.sumizuri.app.data.sources.SourceUpdates
import com.sumizuri.app.source.SourceResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path

// The repos the user added, adding one and removing one, and the updates of the sources installed from them.
class ExtensionsViewModel(
    private val repos: RepoRepository,
    private val updates: SourceUpdates,
    private val importer: SourceFileImporter,
    private val migration: RepoMigration,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ExtensionsState())
    val state: StateFlow<ExtensionsState> = mutableState

    init {
        viewModelScope.launch {
            repos.repos.collect { list -> mutableState.update { it.copy(loaded = true, repos = list) } }
        }
    }

    init {
        viewModelScope.launch { updates.state.collect { now -> mutableState.update { it.copy(updates = now) } } }
    }

    fun onCheck() {
        viewModelScope.launch { updates.check() }
    }

    fun onUpdate(update: SourceUpdate) {
        viewModelScope.launch { updates.update(update) }
    }

    fun onUpdateAll() {
        viewModelScope.launch { updates.updateAll() }
    }

    fun onAddOpen() = mutableState.update { it.copy(adding = AddRepoState()) }

    fun onAddDismiss() = mutableState.update { it.copy(adding = null) }

    fun onAddress(text: String) = mutableState.update { state ->
        state.copy(adding = state.adding?.copy(address = text, problem = null))
    }

    fun onAddConfirm() {
        val dialog = mutableState.value.adding ?: return
        if (dialog.busy || dialog.address.isBlank()) return
        mutableState.update { it.copy(adding = dialog.copy(busy = true, problem = null)) }
        viewModelScope.launch {
            val problem = repos.add(dialog.address)
            mutableState.update { state ->
                state.copy(adding = if (problem == null) null else dialog.copy(busy = false, problem = problem))
            }
        }
    }

    // A source file was picked. It is read and started once, and then waits for the person to name it and choose its kind.
    fun onImportPicked(path: Path) {
        mutableState.update { it.copy(importing = ImportSourceState(busy = true)) }
        viewModelScope.launch {
            val read = importer.read(path)
            mutableState.update { it.copy(importing = read) }
        }
    }

    fun onImportName(text: String) =
        mutableState.update { state -> state.copy(importing = state.importing?.copy(name = text)) }

    fun onImportKind(kind: Kind) =
        mutableState.update { state -> state.copy(importing = state.importing?.copy(kind = kind)) }

    fun onImportDismiss() = mutableState.update { it.copy(importing = null) }

    fun onImportConfirm() {
        val draft = mutableState.value.importing?.takeIf { it.usable } ?: return
        mutableState.update { it.copy(importing = null) }
        viewModelScope.launch { importer.install(draft) }
    }

    fun onRemoveAsk(repo: Repo) = mutableState.update { it.copy(removing = repo) }

    fun onRemoveDismiss() = mutableState.update { it.copy(removing = null) }

    fun onRemoveConfirm() {
        val repo = mutableState.value.removing ?: return
        mutableState.update { it.copy(removing = null) }
        viewModelScope.launch { repos.remove(repo.url) }
    }

    // Opens the move of the sources of a repo to another.
    fun onMoveAsk(repo: Repo) = mutableState.update { it.copy(moving = RepoMoveState(repo)) }

    fun onMoveDismiss() {
        if (mutableState.value.moving?.busy == true) return
        mutableState.update { it.copy(moving = null) }
    }

    fun onMoveRemoveOld(remove: Boolean) =
        mutableState.update { state -> state.copy(moving = state.moving?.copy(removeOld = remove)) }

    // The repo to move to was picked: its index is read and every source of the old repo is matched in it, before anything changes.
    fun onMoveTarget(to: Repo) {
        val moving = mutableState.value.moving ?: return
        mutableState.update { it.copy(moving = moving.copy(to = to, busy = true, problem = null)) }
        viewModelScope.launch {
            val planned = migration.plan(moving.from, to)
            mutableState.update { state ->
                val now = state.moving ?: return@update state
                when (planned) {
                    is SourceResult.Ok -> state.copy(moving = now.copy(plan = planned.value, busy = false))

                    is SourceResult.Err -> state.copy(
                        moving = now.copy(to = null, busy = false, problem = planned.error.message),
                    )
                }
            }
        }
    }

    // Carries out the plan, then takes the old repo away when the person asked for that.
    fun onMoveConfirm() {
        val moving = mutableState.value.moving ?: return
        val plan = moving.plan ?: return
        if (moving.busy) return
        mutableState.update { it.copy(moving = moving.copy(busy = true)) }
        viewModelScope.launch {
            val result = migration.run(plan) { done, total ->
                mutableState.update { state -> state.copy(moving = state.moving?.copy(done = done, total = total)) }
            }
            if (moving.removeOld) repos.remove(plan.from.url)
            mutableState.update { state -> state.copy(moving = state.moving?.copy(busy = false, result = result)) }
        }
    }
}
