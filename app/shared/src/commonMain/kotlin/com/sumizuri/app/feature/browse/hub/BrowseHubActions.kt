package com.sumizuri.app.feature.browse.hub

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.source.repo.InstalledSource

// What the person can do on Browse: pick what shows, open, pin, turn off, edit or remove a source, and add more.
class BrowseHubActions(
    val onTab: (BrowseTab) -> Unit,
    val onKind: (Kind?) -> Unit,
    val onGrouping: (SourceGrouping) -> Unit,
    val onQuery: (String) -> Unit,
    val onOpenSource: (InstalledSource) -> Unit,
    val onPin: (InstalledSource) -> Unit,
    val onEnabled: (InstalledSource, Boolean) -> Unit,
    val onEditCode: (InstalledSource) -> Unit,
    val onRemove: (InstalledSource) -> Unit,
    val onConfirmRemove: () -> Unit,
    val onCancelRemove: () -> Unit,
    val onUpdateAll: () -> Unit,
    // Opens the search of every source, with the words typed so far.
    val onOpenSearch: (String) -> Unit,
    val onOpenRepo: (Repo) -> Unit,
    val onAddRepo: () -> Unit,
    val onAddFile: () -> Unit,
    val onNewSource: () -> Unit,
)
