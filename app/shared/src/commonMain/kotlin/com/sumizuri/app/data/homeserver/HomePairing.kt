package com.sumizuri.app.data.homeserver

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.account.DeviceSecret
import com.sumizuri.app.data.account.PairLink
import com.sumizuri.app.data.account.SyncAccount
import com.sumizuri.app.data.account.SyncAccountStore
import com.sumizuri.app.data.account.SyncClient
import com.sumizuri.app.data.account.SyncProblem
import com.sumizuri.app.data.account.thisDevice
import com.sumizuri.sync.core.AccountKeys
import com.sumizuri.sync.core.SetupRequest
import com.sumizuri.sync.core.SigninRequest
import com.sumizuri.sync.core.SyncCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

internal const val HomeFolder = "home-server"
private const val OwnerFile = "owner.json"
private const val OwnerName = "sumizuri"
private const val PasswordBytes = 32
private const val LocalStart = "http://127.0.0.1:"

// The sign in the computer made for itself, so nobody has to make or remember a password to pair a phone.
@Serializable
private class Owner(val password: String, val recoveryKey: String)

@Serializable
private class Kept(val sealed: String? = null, val owner: Owner? = null)

// Makes this computer the server of its person's phones, and the codes they scan.
class HomePairing(
    private val appFolder: Path,
    private val store: SyncAccountStore,
    private val host: HomeHost,
    private val client: SyncClient = SyncClient(),
    private val system: FileSystem = FileSystem.SYSTEM,
) {
    private val ownerFile = appFolder / HomeFolder / OwnerFile
    private val json = Json { ignoreUnknownKeys = true }

    // A code for one phone, with the account this computer syncs with, which is made the first time.
    suspend fun invite(): Pair<SyncAccount, PairLink> = withContext(Dispatchers.IO) {
        val signedIn = store.account.value
        if (signedIn != null && !isHomeAccount(signedIn)) {
            throw SyncProblem(OtherServer, "This computer already syncs with another server.")
        }
        val up = host.start() ?: throw SyncProblem(NotStarted, "The server of this computer could not start.")
        val account = signedIn ?: enter(up)
        val addresses = up.addresses()
        if (addresses.isEmpty()) throw SyncProblem(NoHomeNetwork, "This computer is on no home network.")
        val secret = client.offerPairing(account)
        account to PairLink(addresses, up.fingerprint, secret)
    }

    // Starts the server again with the app when this computer is one, so paired phones keep syncing.
    suspend fun resume() {
        val account = store.account.value ?: return
        if (!isHomeAccount(account)) return
        if (withContext(Dispatchers.IO) { host.start() } ==
            null
        ) {
            AppLog.warn("homeserver", "paired phones cannot sync until the server starts")
        }
    }

    private fun isHomeAccount(account: SyncAccount): Boolean =
        account.fingerprint == null && account.server.startsWith(LocalStart) && system.exists(ownerFile)

    private suspend fun enter(up: HomeUp): SyncAccount {
        val info = client.info(up.local)
        val device = thisDevice()
        val signed = if (info.needsSetup) {
            val code = up.setupCode ?: throw SyncProblem(NotStarted, "The server gave no setup code.")
            val password = SyncCrypto.base64(SyncCrypto.randomBytes(PasswordBytes))
            val made = AccountKeys.make(OwnerName, password)
            // Written first, so a crash right after claiming never leaves a server nobody can enter.
            write(Owner(password, made.recoveryKey))
            client.setup(up.local, SetupRequest(code, made.request, device)) to made.accountKey
        } else {
            val owner =
                read() ?: throw SyncProblem(NotStarted, "The sign in of this computer's server could not be opened.")
            val keys = AccountKeys.signInKeys(owner.password, client.salt(up.local, OwnerName))
            val signed = client.signin(up.local, SigninRequest(OwnerName, SyncCrypto.base64(keys.proof), device))
            val key = AccountKeys.openAccountKey(keys.unlock, signed.keyByPassword)
                ?: throw SyncProblem(NotStarted, "The account key could not be opened.")
            signed to key
        }
        val (answer, key) = signed
        val account =
            SyncAccount(
                up.local,
                info.name,
                answer.userId,
                answer.username,
                answer.role,
                answer.deviceId,
                answer.token,
                SyncCrypto.base64(key),
            )
        store.save(account)
        return account
    }

    private fun read(): Owner? = runCatching {
        val kept = json.decodeFromString(Kept.serializer(), system.read(ownerFile) { readUtf8() })
        val sealed = kept.sealed ?: return kept.owner
        val opened = DeviceSecret.open(SyncCrypto.fromBase64(sealed)) ?: return null
        json.decodeFromString(Owner.serializer(), opened.decodeToString())
    }.getOrNull()

    private fun write(owner: Owner) {
        val sealed = DeviceSecret.seal(json.encodeToString(Owner.serializer(), owner).encodeToByteArray())
        val kept = if (sealed == null) Kept(owner = owner) else Kept(sealed = SyncCrypto.base64(sealed))
        val folder = appFolder / HomeFolder
        system.createDirectories(folder)
        val part = folder / "$OwnerFile.tmp"
        system.write(part) { writeUtf8("") }
        DeviceSecret.keepPrivate(part.toString())
        system.write(part) { writeUtf8(json.encodeToString(Kept.serializer(), kept)) }
        system.atomicMove(part, ownerFile)
    }

    companion object {
        const val OtherServer = "pair_other_server"
        const val NotStarted = "pair_not_started"
        const val NoHomeNetwork = "pair_no_network"
    }
}
