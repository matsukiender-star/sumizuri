package com.sumizuri.app.data.account

import kotlinx.serialization.Serializable

// One sync as the details page lists it: when, whether it worked, how many titles changed here, whether and how much it sent, or why it failed.
@Serializable
data class SyncLogEntry(
    val at: Long,
    val ok: Boolean,
    val changedHere: Int = 0,
    val sent: Boolean = false,
    val bytes: Long = 0,
    val message: String = "",
    val conflicts: List<SyncConflict> = emptyList(),
)

// How a sync of this profile went last, for the sync page.
sealed interface SyncRun {
    data object Idle : SyncRun
    data object Running : SyncRun
    data class Done(
        val at: Long,
        val changedHere: Int,
        val sent: Boolean,
        val bytes: Long = 0,
        val conflicts: List<SyncConflict> = emptyList(),
    ) : SyncRun
    data class Failed(val problem: SyncProblem) : SyncRun
}
