package com.sumizuri.app.feature.sync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.account.LibrarySyncer
import com.sumizuri.app.data.account.SyncAccount
import com.sumizuri.app.data.account.SyncAccountStore
import com.sumizuri.app.data.account.SyncClient
import com.sumizuri.app.data.account.SyncProblem
import com.sumizuri.app.data.account.SyncRun
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.feature.sync.data.ServerCopy
import com.sumizuri.app.feature.sync.signin.SyncViewModel
import com.sumizuri.sync.core.AccountDelete
import com.sumizuri.sync.core.AccountKeys
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.LibraryKeys
import com.sumizuri.sync.core.OpenLibrary
import com.sumizuri.sync.core.PasswordChange
import com.sumizuri.sync.core.Role
import com.sumizuri.sync.core.SyncCrypto
import com.sumizuri.sync.core.TwoStepConfirm
import com.sumizuri.sync.core.TwoStepOff
import com.sumizuri.sync.core.TwoStepSecret
import com.sumizuri.sync.core.TwoStepStart
import com.sumizuri.sync.core.UserChange
import com.sumizuri.sync.core.UsernameChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// What a person changes about their own account from this device: the password, the name, the recovery key, two-step sign in, and deleting it. Each call proves the person with the password, and the page shows its progress and problems.
class AccountChanges internal constructor(
    private val account: SyncAccount,
    private val store: SyncAccountStore,
    private val client: SyncClient,
    private val accountKey: ByteArray,
    private val mutable: MutableStateFlow<SyncHome>,
    private val work: (suspend () -> Unit) -> Unit,
) {
    // A new password for this account: the current one proves it is the person, and the new one locks the account key again.
    fun changePassword(current: String, new: String, signOutOthers: Boolean, code: String, onDone: () -> Unit) = work {
        val proof = SyncCrypto.fromBase64(proofOf(current))
        val password = withContext(Dispatchers.Default) { AccountKeys.newPassword(new, accountKey) }
        client.changePassword(account, PasswordChange(SyncCrypto.base64(proof), password, signOutOthers, codeOf(code)))
        if (signOutOthers) mutable.update { it.copy(devices = client.devices(account)) }
        onDone()
    }

    // A new username, given the password. The account kept on this device takes the new name.
    fun changeUsername(password: String, name: String, code: String, onDone: () -> Unit) = work {
        client.changeUsername(account, UsernameChange(proofOf(password), name.trim(), codeOf(code)))
        store.save((store.account.value ?: account).copy(username = name.trim()))
        onDone()
    }

    // A new recovery key, given the password. The old key stops working, and the new one is handed back once to be kept.
    fun newRecoveryKey(password: String, code: String, onKey: (String) -> Unit) = work {
        val (change, key) = withContext(Dispatchers.Default) { AccountKeys.newRecovery(accountKey) }
        client.changeRecovery(account, change.copy(currentProof = proofOf(password), code = codeOf(code)))
        onKey(key)
    }

    // Deletes the account on the server with all it keeps there, given the password. This device then forgets the account, and its own library stays.
    fun deleteAccount(password: String, code: String, onDone: () -> Unit) = work {
        client.deleteAccount(account, AccountDelete(proofOf(password), codeOf(code)))
        store.clear()
        onDone()
    }

    // Starts turning on two-step sign in, given the password, and hands back the secret for the authenticator app.
    fun startTwoStep(password: String, onSecret: (TwoStepSecret) -> Unit) = work {
        onSecret(client.startTwoStep(account, TwoStepStart(proofOf(password))))
    }

    // The first code from the authenticator app turns two-step sign in on.
    fun confirmTwoStep(code: String, onDone: () -> Unit) = work {
        client.confirmTwoStep(account, TwoStepConfirm(code.trim()))
        mutable.update { it.copy(twoStep = true, askCode = true) }
        onDone()
    }

    // Turns two-step sign in off, given the password and a code.
    fun turnOffTwoStep(password: String, code: String, onDone: () -> Unit) = work {
        client.turnOffTwoStep(account, TwoStepOff(proofOf(password), codeOf(code)))
        mutable.update { it.copy(twoStep = false, askCode = false) }
        onDone()
    }

    private fun codeOf(code: String): String? = code.trim().ifEmpty { null }

    // The proof of the password, from the salt of the account's name as it is now, which may have changed since the page opened.
    private suspend fun proofOf(password: String): String {
        val name = store.account.value?.username ?: account.username
        val salt = client.salt(account.server, name)
        return SyncCrypto.base64(withContext(Dispatchers.Default) { AccountKeys.signInKeys(password, salt).proof })
    }
}
