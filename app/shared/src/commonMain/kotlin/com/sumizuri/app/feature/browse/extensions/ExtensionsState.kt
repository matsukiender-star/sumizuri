package com.sumizuri.app.feature.browse.extensions

import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.sources.RepoMovePlan
import com.sumizuri.app.data.sources.RepoMoveResult
import com.sumizuri.app.data.sources.RepoProblem
import com.sumizuri.app.data.sources.UpdatesState

data class ExtensionsState(
    // False until the repos have been read, so the empty hint does not flash.
    val loaded: Boolean = false,
    val repos: List<Repo> = emptyList(),
    // Set while the dialog that adds a repo is open.
    val adding: AddRepoState? = null,
    // The repo that is waiting for a yes before it is removed.
    val removing: Repo? = null,
    // Which installed sources have a newer version, and how the check and the updating are going.
    val updates: UpdatesState = UpdatesState(),
    // A source file the person picked, while it is read and while it waits for a yes.
    val importing: ImportSourceState? = null,
    // The move of the sources of a repo to another, while its window is open.
    val moving: RepoMoveState? = null,
)

// The dialog that asks for a repo's address, with the text so far, a check under way, and what went wrong.
data class AddRepoState(val address: String = "", val busy: Boolean = false, val problem: RepoProblem? = null)

// The move of the sources of one repo to another: the repo they leave, the one picked to take them, the plan once it is worked out, how far the move has got and what it did, and whether the old repo goes once they moved.
data class RepoMoveState(
    val from: Repo,
    val to: Repo? = null,
    val plan: RepoMovePlan? = null,
    val busy: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val result: RepoMoveResult? = null,
    val problem: String? = null,
    val removeOld: Boolean = true,
)
