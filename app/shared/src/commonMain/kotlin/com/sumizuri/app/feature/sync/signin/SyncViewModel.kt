package com.sumizuri.app.feature.sync.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.account.SyncAccount
import com.sumizuri.app.data.account.SyncAccountStore
import com.sumizuri.app.data.account.SyncClient
import com.sumizuri.app.data.account.SyncProblem
import com.sumizuri.app.data.account.thisDevice
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.sync.core.AccountKeys
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.LibraryKeys
import com.sumizuri.sync.core.OpenLibrary
import com.sumizuri.sync.core.RecoverRequest
import com.sumizuri.sync.core.RecoveryCode
import com.sumizuri.sync.core.RecoveryKeyRequest
import com.sumizuri.sync.core.ServerInfo
import com.sumizuri.sync.core.SetupRequest
import com.sumizuri.sync.core.SignedIn
import com.sumizuri.sync.core.SigninRequest
import com.sumizuri.sync.core.SignupRequest
import com.sumizuri.sync.core.SyncCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// What the person does with a server once it answered.
enum class SyncMode { Setup, SignIn, Create, Recover }

// Where the sync page is: asking for the address, at a server, showing the new recovery key, or signed in.
sealed interface SyncStep {
    data object Address : SyncStep

    // askCode is set once the server asked for the code of two-step sign in.
    data class Server(val address: String, val info: ServerInfo, val mode: SyncMode, val askCode: Boolean = false) :
        SyncStep
    data class SaveRecovery(val recoveryKey: String, val server: String, val username: String) : SyncStep

    // Which profile on the server this profile syncs with, asked once after signing in. With none on the server yet, only a new one is offered. twoStepOff tells that a sign in with the recovery key turned two-step sign in off.
    data class ChooseProfile(
        val account: SyncAccount,
        val profiles: List<OpenLibrary>,
        val suggested: String,
        val twoStepOff: Boolean = false,
    ) : SyncStep
    data class Connected(val account: SyncAccount) : SyncStep
}

data class SyncUi(val step: SyncStep, val busy: Boolean = false, val problem: SyncProblem? = null)

// The sync page: connects to a server, sets it up or signs in, and keeps the account. The slow key work runs away from the screen.
class SyncViewModel(
    private val store: SyncAccountStore,
    private val settings: SettingsRepository,
    // The name of the profile open on this device, offered as the name of a new profile on the server.
    private val profileName: String,
    private val client: SyncClient = SyncClient(),
) : ViewModel() {
    private val mutable =
        MutableStateFlow(SyncUi(store.account.value?.let { SyncStep.Connected(it) } ?: SyncStep.Address))
    val ui: StateFlow<SyncUi> = mutable

    // Signing out, here or by the server, takes the page back to the address.
    init {
        viewModelScope.launch {
            store.account.collect { account ->
                if (account == null &&
                    mutable.value.step is SyncStep.Connected
                ) {
                    mutable.value = SyncUi(SyncStep.Address)
                }
            }
        }
    }

    fun connect(typed: String) = work {
        val address = SyncClient.addressOf(typed)
        val info = client.info(address)
        SyncStep.Server(address, info, if (info.needsSetup) SyncMode.Setup else SyncMode.SignIn)
    }

    fun choose(mode: SyncMode) {
        val step = mutable.value.step as? SyncStep.Server ?: return
        mutable.value = SyncUi(step.copy(mode = mode))
    }

    fun back() {
        mutable.value = SyncUi(SyncStep.Address)
    }

    // Claims a new server with the code from its log, which makes this the owner's account.
    fun setup(code: String, username: String, password: String) = atServer { server ->
        val made = withContext(Dispatchers.Default) { AccountKeys.make(username, password) }
        val signed = client.setup(server.address, SetupRequest(code.trim(), made.request, thisDevice()))
        keep(server, signed, made.accountKey)
        SyncStep.SaveRecovery(made.recoveryKey, server.address, signed.username)
    }

    fun create(username: String, password: String, invite: String) = atServer { server ->
        val made = withContext(Dispatchers.Default) { AccountKeys.make(username, password) }
        val signed = client.signup(
            server.address,
            SignupRequest(made.request, thisDevice(), invite.trim().ifEmpty { null }),
        )
        keep(server, signed, made.accountKey)
        SyncStep.SaveRecovery(made.recoveryKey, server.address, signed.username)
    }

    fun signIn(username: String, password: String, code: String) = atServer { server ->
        val salt = client.salt(server.address, username.trim())
        val keys = withContext(Dispatchers.Default) { AccountKeys.signInKeys(password, salt) }
        val signed = client.signin(
            server.address,
            SigninRequest(
                username.trim(),
                SyncCrypto.base64(
                    keys.proof,
                ),
                thisDevice(),
                code.trim().ifEmpty {
                    null
                },
            ),
        )
        val accountKey =
            AccountKeys.openAccountKey(keys.unlock, signed.keyByPassword)
                ?: throw SyncProblem(KeyMismatch, "The account key could not be opened.")
        askProfile(keep(server, signed, accountKey))
    }

    // Opens the account key with the recovery key, locks it again with the new password, and signs in.
    fun recover(username: String, recoveryText: String, password: String) = atServer { server ->
        val recovery =
            RecoveryCode.parse(recoveryText) ?: throw SyncProblem(BadRecoveryKey, "That is not a recovery key.")
        val keys = SyncCrypto.recoveryKeys(recovery)
        val proof = SyncCrypto.base64(keys.proof)
        val locked = client.recoveryKey(server.address, RecoveryKeyRequest(username.trim(), proof))
        val accountKey =
            SyncCrypto.unlock(keys.unlock, SyncCrypto.fromBase64(locked.keyByRecovery), SyncCrypto.AccountKeyPurpose)
                ?: throw SyncProblem(KeyMismatch, "The account key could not be opened.")
        val newPassword = withContext(Dispatchers.Default) { AccountKeys.newPassword(password, accountKey) }
        val signed = client.recover(server.address, RecoverRequest(username.trim(), proof, newPassword, thisDevice()))
        (askProfile(keep(server, signed, accountKey)) as SyncStep.ChooseProfile).copy(twoStepOff = signed.twoStepOff)
    }

    // The recovery key was saved, so the page moves on to the account.
    fun savedRecovery() {
        val account = store.account.value ?: return
        work { askProfile(account) }
    }

    // Syncs this profile with a profile already on the server.
    fun pick(id: Long) {
        val step = mutable.value.step as? SyncStep.ChooseProfile ?: return
        work {
            settings.setText(SettingKey.SyncLibraryId, id.toString())
            SyncStep.Connected(step.account)
        }
    }

    // Makes a new profile on the server and syncs this profile with it.
    fun makeProfile(name: String) {
        val step = mutable.value.step as? SyncStep.ChooseProfile ?: return
        work {
            val made =
                withContext(
                    Dispatchers.Default,
                ) { LibraryKeys.make(name, SyncCrypto.fromBase64(step.account.accountKey)) }
            val entry = client.newLibrary(step.account, made.request)
            settings.setText(SettingKey.SyncLibraryId, entry.id.toString())
            SyncStep.Connected(step.account)
        }
    }

    // Leaves the choice for later, in the sync settings.
    fun skipChoice() {
        val step = mutable.value.step as? SyncStep.ChooseProfile ?: return
        mutable.value = SyncUi(SyncStep.Connected(step.account))
    }

    // The profiles of the account on the server, opened, to ask which one this profile syncs with.
    private suspend fun askProfile(account: SyncAccount): SyncStep {
        val accountKey = SyncCrypto.fromBase64(account.accountKey)
        val profiles =
            withContext(
                Dispatchers.Default,
            ) { client.libraries(account).mapNotNull { LibraryKeys.open(it, accountKey) } }
        return SyncStep.ChooseProfile(account, profiles, profileName)
    }

    fun dismissProblem() = mutable.update { it.copy(problem = null) }

    private fun keep(server: SyncStep.Server, signed: SignedIn, accountKey: ByteArray): SyncAccount {
        val account =
            SyncAccount(
                server.address,
                server.info.name,
                signed.userId,
                signed.username,
                signed.role,
                signed.deviceId,
                signed.token,
                SyncCrypto.base64(accountKey),
            )
        store.save(account)
        return account
    }

    private fun atServer(block: suspend (SyncStep.Server) -> SyncStep) {
        val server = mutable.value.step as? SyncStep.Server ?: return
        work { block(server) }
    }

    private fun work(block: suspend () -> SyncStep) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            mutable.value = try {
                SyncUi(block())
            } catch (problem: SyncProblem) {
                val step = mutable.value.step
                // The password was right and the account has two-step sign in, so the page asks for the code next.
                if (problem.code == ErrorCodes.CodeNeeded && step is SyncStep.Server) {
                    SyncUi(step.copy(askCode = true))
                } else {
                    mutable.value.copy(busy = false, problem = problem)
                }
            }
        }
    }

    companion object {
        const val KeyMismatch = "key_mismatch"
        const val BadRecoveryKey = "bad_recovery_key"
    }
}
