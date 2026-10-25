package com.sumizuri.app.feature.browse.extensions

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.remove
import com.sumizuri.app.shared.resources.repo_remove_message
import com.sumizuri.app.shared.resources.repo_remove_title
import com.sumizuri.app.ui.kit.dialog.ConfirmDialog
import org.jetbrains.compose.resources.stringResource

// Asks before a repo is removed, and says that the sources installed from it stay.
@Composable
internal fun RemoveRepoDialog(repo: Repo, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        stringResource(Res.string.repo_remove_title),
        stringResource(Res.string.repo_remove_message, repo.name),
        stringResource(Res.string.remove),
        onConfirm,
        onDismiss,
    )
}
