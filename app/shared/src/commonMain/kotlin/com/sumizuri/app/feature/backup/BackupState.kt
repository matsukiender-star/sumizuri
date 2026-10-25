package com.sumizuri.app.feature.backup

import com.sumizuri.app.data.backup.BackupApp
import com.sumizuri.app.data.backup.BackupCompression
import com.sumizuri.app.data.backup.BackupPart
import com.sumizuri.app.data.backup.BackupProblem
import com.sumizuri.app.data.backup.BackupSummary
import com.sumizuri.app.data.backup.RestoreResult
import okio.Path

// Where a restore is, from the choice of a file to the end.
sealed interface BackupState {
    data object Idle : BackupState

    // The file is being read.
    data object Reading : BackupState

    // The backup is shut with a password, which was not right the last time when wrong is true.
    data class Password(val wrong: Boolean) : BackupState

    // The backup is read and waits for the person to say yes. Settings is how many app settings it brings, which the person may leave out.
    data class Confirm(val app: BackupApp, val titles: Int, val shelves: Int, val settings: Int = 0) : BackupState

    // Putting titles in, or looking for the sources of those that have none.
    data class Running(val matching: Boolean, val done: Int, val total: Int, val cancelling: Boolean) : BackupState

    // Finished or stopped. Matched is null when no title was left to look a source for, and waiting holds the titles that still have no source, which can be taken to the page that moves them.
    data class Done(val result: RestoreResult, val matched: Int?, val waiting: List<Long> = emptyList()) : BackupState

    data class Failed(val problem: BackupProblem) : BackupState
}

// Where the making of a backup is.
sealed interface MakeState {
    data object Idle : MakeState

    data class Writing(val done: Int, val total: Int) : MakeState

    // The file is made and waits for the person to pick where it goes.
    data class Ready(val file: Path, val name: String, val summary: BackupSummary) : MakeState

    data class Saved(val summary: BackupSummary) : MakeState

    data object NotSaved : MakeState

    data object Failed : MakeState
}

// What goes into a backup and how it is packed, kept for the next one. The password is never kept.
data class MakeChoices(
    val parts: Set<BackupPart> = setOf(BackupPart.Sources, BackupPart.SourceSettings, BackupPart.Settings),
    val compression: BackupCompression = BackupCompression.Fast,
)

// What the backup page can ask for.
internal class BackupCalls(
    val onBack: () -> Unit,
    val onPicked: (Path) -> Unit,
    val onPassword: (String) -> Unit,
    val onConfirm: (withSettings: Boolean) -> Unit,
    val onCancel: () -> Unit,
    val onDismiss: () -> Unit,
    val onMigrate: (List<Long>) -> Unit,
    val onMake: (MakeChoices, password: String?) -> Unit,
    val onSaved: (Boolean) -> Unit,
    val onMakeDismiss: () -> Unit,
    val onStopMaking: () -> Unit = {},
)
