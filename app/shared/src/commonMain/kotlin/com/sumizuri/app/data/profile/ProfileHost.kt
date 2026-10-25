package com.sumizuri.app.data.profile

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.AppContainer
import com.sumizuri.app.data.account.SyncAccountStore
import com.sumizuri.app.data.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path

// How long the screens of the profile that was left get to let go of its database before it is closed.
private const val LetGoMs = 600L

// Holds the container of the open profile and opens another one when the person switches. The app draws whatever container is current, so a switch is a new container and a fresh set of screens.
class ProfileHost(val registry: ProfileRegistry, private val open: (Path) -> AppContainer) {
    private val lock = Mutex()

    // The switch is not tied to the screen that asked for it, since that screen ends with the switch.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // The network setting is read first, so no network client is made before the router is in place.
    init {
        Network.load(registry.networkFile)
    }

    // The sync account, one for the whole app, shared by the profile page and the sync page so both always agree. It comes before the first container, which is given it.
    val syncAccounts: SyncAccountStore by lazy { SyncAccountStore(registry.syncFile) }
    private val mutable = MutableStateFlow(opened(registry.fileOf(registry.state.value.active)))
    val container: StateFlow<AppContainer> = mutable
    val state: StateFlow<RegistryState> = registry.state

    // A container gets the sync account of the app, since one account serves every profile.
    private fun opened(file: Path): AppContainer = open(file).also { it.librarySync.accounts = syncAccounts }

    fun switchTo(id: Long) {
        scope.launch {
            lock.withLock {
                val now = registry.state.value
                val target = now.profiles.firstOrNull { it.id == id } ?: return@withLock
                if (target.id == now.activeId) return@withLock
                val old = mutable.value
                registry.setActive(id)
                mutable.value = opened(registry.fileOf(target))
                AppLog.info("profile", "switched to ${target.name}")
                delay(LetGoMs)
                old.close()
            }
        }
    }

    // Makes a profile from the open one without its library, and opens it. Gives null when the file could not be made.
    suspend fun create(name: String, avatar: String?): Profile? {
        val made = makeProfile(name, avatar)
        if (made != null) switchTo(made.id)
        return made
    }

    private suspend fun makeProfile(name: String, avatar: String?): Profile? = lock.withLock {
        val now = registry.state.value
        val file = profileFileName(name, now.profiles.mapNotNull { it.file })
        try {
            cloneWithoutLibrary(registry.fileOf(now.active), registry.profilesFolder / file)
        } catch (error: Throwable) {
            AppLog.error("profile", "could not make the profile $name", error)
            return@withLock null
        }
        registry.add(name, avatar, file)
    }

    // Removes a profile that is not open, with its file and its picture. Gives false for the one that is open or the only one.
    suspend fun delete(id: Long): Boolean = lock.withLock {
        if (registry.state.value.activeId == id) return@withLock false
        val doomed = registry.remove(id) ?: return@withLock false
        registry.deleteFiles(doomed)
        registry.deleteAvatar(doomed.avatar)
        AppLog.info("profile", "deleted ${doomed.name}")
        true
    }

    fun rename(id: Long, name: String) = registry.rename(id, name)

    fun setAvatar(id: Long, avatar: String?) {
        val old = registry.state.value.profiles
            .firstOrNull { it.id == id }
            ?.avatar
        registry.setAvatar(id, avatar)
        if (old != avatar) registry.deleteAvatar(old)
    }
}
