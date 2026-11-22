package com.sumizuri.app.data.homeserver

import com.sumizuri.app.data.account.SyncAccount
import com.sumizuri.app.data.account.SyncClient
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.sync.core.LibraryKeys
import com.sumizuri.sync.core.SyncCrypto

// The computer puts its open profile on a server that has none yet. It never takes one that is there by itself, since that would mix two profiles of the computer.
internal suspend fun linkComputer(
    client: SyncClient,
    settings: SettingsRepository,
    account: SyncAccount,
    profileName: String,
): Boolean {
    val there = client.libraries(account)
    val linked = settings.textNow(SettingKey.SyncLibraryId)?.toLongOrNull()
    return when {
        there.isEmpty() -> {
            make(client, settings, account, profileName)
            true
        }

        there.any { it.id == linked } -> true

        else -> false
    }
}

// The phone takes the one profile of the computer, so one scan is all it takes. With several, the sync page asks which.
internal suspend fun linkPhone(
    client: SyncClient,
    settings: SettingsRepository,
    account: SyncAccount,
    profileName: String,
): Boolean {
    // A number left from another server would name someone else's profile on this one.
    settings.resetNames(SettingKey.SyncLibraryId)
    val there = client.libraries(account)
    when (there.size) {
        0 -> make(client, settings, account, profileName)
        1 -> settings.setText(SettingKey.SyncLibraryId, there.single().id.toString())
        else -> return false
    }
    return true
}

private suspend fun make(client: SyncClient, settings: SettingsRepository, account: SyncAccount, profileName: String) {
    val request = LibraryKeys.make(profileName, SyncCrypto.fromBase64(account.accountKey)).request
    settings.setText(SettingKey.SyncLibraryId, client.newLibrary(account, request).id.toString())
}
