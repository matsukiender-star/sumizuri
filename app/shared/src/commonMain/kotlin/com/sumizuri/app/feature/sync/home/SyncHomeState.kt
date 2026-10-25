package com.sumizuri.app.feature.sync.home

import com.sumizuri.app.data.account.SyncProblem
import com.sumizuri.app.feature.sync.data.ServerCopy
import com.sumizuri.sync.core.AdminOverview
import com.sumizuri.sync.core.DeviceEntry
import com.sumizuri.sync.core.InviteEntry
import com.sumizuri.sync.core.LibraryEntry
import com.sumizuri.sync.core.LogEntry
import com.sumizuri.sync.core.OpenLibrary
import com.sumizuri.sync.core.ServerInfo
import com.sumizuri.sync.core.ServerSettingEntry
import com.sumizuri.sync.core.UserDetail
import com.sumizuri.sync.core.UserEntry

// The parts of the signed in sync page. Admin only shows for the owner and admins.
enum class SyncTab { Account, Library, Updates, History, Admin }

// Whether the server of a signed in account answers right now.
sealed interface ServerStatus {
    data object Checking : ServerStatus
    data class Online(val version: String) : ServerStatus
    data class Offline(val problem: SyncProblem) : ServerStatus
}

// What the signed in page shows. A list is nothing until it was fetched once.
data class SyncHome(
    val tab: SyncTab = SyncTab.Account,
    val status: ServerStatus = ServerStatus.Checking,
    // Why the server was last found down, kept while it is asked again and cleared once it answers.
    val down: SyncProblem? = null,
    val devices: List<DeviceEntry>? = null,
    val libraries: List<OpenLibrary>? = null,
    // The same libraries as the server lists them, with the details of their snapshots.
    val entries: List<LibraryEntry>? = null,
    // The server copy of the library this profile links to, once fetched, and whether it was looked for.
    val copy: ServerCopy? = null,
    val copyChecked: Boolean = false,
    // How far packing the library has got while it is sent, as done and total titles.
    val packing: Pair<Int, Int>? = null,
    val users: List<UserEntry>? = null,
    val invites: List<InviteEntry>? = null,
    // The server at a glance, and the person open on the page of one person, for the owner and admins.
    val overview: AdminOverview? = null,
    val person: UserDetail? = null,
    // The change log of the server, newest first, once it was read.
    val log: List<LogEntry>? = null,
    // The live settings of the server, for the owner and admins, and what the server said about itself when it was last asked.
    val serverSettings: List<ServerSettingEntry>? = null,
    val info: ServerInfo? = null,
    // A new invite code, shown once until the page moves on.
    val newInvite: String? = null,
    // Whether this account has two-step sign in, once read, and whether the pages that need the password ask for the code too.
    val twoStep: Boolean? = null,
    val askCode: Boolean = false,
    val busy: Boolean = false,
    val problem: SyncProblem? = null,
)
