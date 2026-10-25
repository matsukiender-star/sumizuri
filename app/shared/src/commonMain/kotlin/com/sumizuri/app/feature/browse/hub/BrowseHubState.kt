package com.sumizuri.app.feature.browse.hub

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.feature.browse.repo.RemovePrompt
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.browse_tab_extensions
import com.sumizuri.app.shared.resources.browse_tab_sources
import com.sumizuri.app.source.repo.InstalledSource
import org.jetbrains.compose.resources.StringResource

data class BrowseHubState(
    // False until the installed sources have been read, so the empty page does not flash.
    val loaded: Boolean = false,
    val tab: BrowseTab = BrowseTab.Sources,
    // The kinds that have a source installed, and the one on show, or null for all of them.
    val kinds: Set<Kind> = emptySet(),
    val kind: Kind? = null,
    val grouping: SourceGrouping = SourceGrouping.Repo,
    // The words that narrow the installed sources by name or site.
    val query: String = "",
    // The pinned sources, then the ones used lately, as a row of shortcuts above the groups.
    val jump: List<InstalledSource> = emptyList(),
    val pinned: Set<String> = emptySet(),
    val groups: List<SourceGroup> = emptyList(),
    // The sources that are turned off, shown faded at the end so they can be turned on again.
    val off: List<InstalledSource> = emptyList(),
    val sourceCount: Int = 0,
    // The installed sources that have a newer version in their repo, and whether they are being updated.
    val updateIds: Set<String> = emptySet(),
    val updating: Boolean = false,
    // Asks before removing a source that titles in the library came from.
    val removing: RemovePrompt? = null,
) {
    val updateCount: Int get() = updateIds.size
}

// The two halves of Browse, the sources that are installed and the repos they come from.
enum class BrowseTab(val label: StringResource) {
    Sources(Res.string.browse_tab_sources),
    Extensions(Res.string.browse_tab_extensions),
}

// How the installed sources are put under headings: by the repo they came from, by language, or all under one.
enum class SourceGrouping { Repo, Language, None }

// What the heading of a group names.
sealed interface GroupName {
    data class Repo(val name: String) : GroupName
    data class Language(val code: String) : GroupName

    // Sources added from a file or written in the app, which no repo keeps up to date.
    data object ByHand : GroupName

    // The source that reads the files on this device.
    data object Device : GroupName
    data object All : GroupName
}

// The sources shown under one heading, with a key that stays the same while the list changes.
data class SourceGroup(val key: String, val name: GroupName, val sources: List<InstalledSource>)
