package com.sumizuri.app.data.profile

import com.sumizuri.app.data.text.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

private const val RegistryName = "profiles.json"
private const val ProfilesFolder = "profiles"
private const val AvatarsFolder = "avatars"
private const val FirstName = "Profile 1"

// The folders beside the profiles that keep one folder for each, called after its file.
private val FoldersByName = listOf("backups", "downloads", "covers", "pages")

@Serializable
private class Saved(val profiles: List<SavedProfile>, val active: Long)

@Serializable
private class SavedProfile(
    val id: Long,
    val name: String,
    val avatar: String? = null,
    val createdAt: Long,
    val file: String? = null,
)

// The list of profiles and which one is open, kept in one small file beside the files of the profiles. The first profile keeps the file the app had before profiles, so nothing is moved or copied for people who already have a library.
class ProfileRegistry(
    private val folder: Path,
    private val legacyFile: Path,
    private val system: FileSystem = FileSystem.SYSTEM,
) {
    private val registryFile = folder / RegistryName
    private val json = Json { ignoreUnknownKeys = true }

    val profilesFolder: Path = folder / ProfilesFolder
    val avatarsFolder: Path = folder / AvatarsFolder

    // The sync account belongs to the app, not to one profile, so it sits beside the profiles.
    val syncFile: Path = folder / "sync.json"

    // The proxy the whole app goes through, which belongs to this device like the sync account.
    val networkFile: Path = folder / "network.json"

    private val mutable = MutableStateFlow(load())
    val state: StateFlow<RegistryState> = mutable

    private fun load(): RegistryState {
        system.createDirectories(folder)
        system.createDirectories(profilesFolder)
        val saved = if (system.exists(registryFile)) {
            runCatching {
                json.decodeFromString(
                    Saved.serializer(),
                    system.read(registryFile) { readUtf8() },
                )
            }.getOrNull()
        } else {
            null
        }
        val profiles = saved?.profiles?.map { Profile(it.id, it.name, it.avatar, it.createdAt, it.file) }.orEmpty()
        if (profiles.isEmpty()) {
            val since = system.metadataOrNull(legacyFile)?.createdAtMillis ?: nowMillis()
            return RegistryState(listOf(Profile(1, FirstName, null, since, null)), 1).also(::save)
        }
        val open = saved?.active?.takeIf { id -> profiles.any { it.id == id } } ?: profiles.first().id
        return RegistryState(profiles, open).also(::save)
    }

    private fun save(state: RegistryState) {
        val saved =
            Saved(state.profiles.map { SavedProfile(it.id, it.name, it.avatar, it.createdAt, it.file) }, state.activeId)
        val temporary = folder / "$RegistryName.tmp"
        system.write(temporary) { writeUtf8(json.encodeToString(Saved.serializer(), saved)) }
        system.atomicMove(temporary, registryFile)
    }

    // The file a profile keeps its library in.
    fun fileOf(profile: Profile): Path = profile.file?.let { profilesFolder / it } ?: legacyFile

    // The file names a new profile may not take: those in use, and those of deleted profiles that left something behind, which the new one would otherwise open as its own.
    fun takenFiles(): List<String> {
        val inFolders = FoldersByName
            .flatMap { system.listOrNull(profilesFolder / it).orEmpty() }
            .map { it.name + ProfileExtension }
        val beside = system
            .listOrNull(profilesFolder)
            .orEmpty()
            .map { it.name }
            .filter { ProfileExtension in it }
            .map { it.substringBeforeLast(ProfileExtension) + ProfileExtension }
        return mutable.value.profiles.mapNotNull { it.file } + inFolders + beside
    }

    private fun change(edit: (RegistryState) -> RegistryState) {
        val next = edit(mutable.value)
        save(next)
        mutable.value = next
    }

    // Puts a profile at the end of the list, with a file that already exists.
    fun add(name: String, avatar: String?, file: String): Profile {
        val profile = Profile(mutable.value.profiles.maxOf { it.id } + 1, name, avatar, nowMillis(), file)
        change { it.copy(profiles = it.profiles + profile) }
        return profile
    }

    // Only the name changes. The file keeps the one it was made with, as downloads, backups, covers, the journal and the sync base are all found by it, and covers are saved as whole paths.
    fun rename(id: Long, name: String) = change { now ->
        now.copy(profiles = now.profiles.map { if (it.id == id) it.copy(name = name) else it })
    }

    fun setAvatar(id: Long, avatar: String?) = change { now ->
        now.copy(
            profiles = now.profiles.map {
                if (it.id ==
                    id
                ) {
                    it.copy(avatar = avatar)
                } else {
                    it
                }
            },
        )
    }

    fun setActive(id: Long) = change { it.copy(activeId = id) }

    // Takes a profile off the list and gives it back, or null when it is the only one. Its files are for the caller to remove.
    fun remove(id: Long): Profile? {
        val now = mutable.value
        val doomed = now.profiles.firstOrNull { it.id == id } ?: return null
        if (now.profiles.size <= 1) return null
        val rest = now.profiles.filter { it.id != id }
        change { it.copy(profiles = rest, activeId = if (it.activeId == id) rest.first().id else it.activeId) }
        return doomed
    }

    // A path in the avatars folder for a new picture.
    fun newAvatarPath(): Path {
        system.createDirectories(avatarsFolder)
        return avatarsFolder / "${nowMillis()}.png"
    }

    // Removes a picture, but only one this folder made.
    fun deleteAvatar(path: String?) {
        if (path == null) return
        val target = path.toPath()
        if (target.parent == avatarsFolder) runCatching { system.delete(target) }
    }

    // Removes the file of a profile, with the journal beside it. The first profile has the file the app had before profiles.
    fun deleteFiles(profile: Profile) {
        val file = fileOf(profile)
        for (suffix in listOf("", "-wal", "-shm")) runCatching { system.delete(file.parent!! / (file.name + suffix)) }
    }
}
