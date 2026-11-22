package com.sumizuri.app.data.profile

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.backup.backupFolder
import com.sumizuri.app.data.download.downloadFolder
import com.sumizuri.app.data.library.coverFolder
import com.sumizuri.app.ui.kit.image.snapshotFolder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// What sits beside a database under its exact name.
private val BesideEndings = listOf("", "-wal", "-shm", "-journal", ".lck")

// The sync bases add the account and the library after this.
private const val SyncBaseStart = ".sync-"

// The channels of the Live tab, beside the database like the rest.
internal const val LiveFolder = "live"

// Deletes for good what one profile in the trash owns, and nothing that others share. Links are removed and never followed. Gives whether nothing of it is left.
internal fun purgeProfileFiles(
    doomed: Profile,
    others: List<Profile>,
    fileOf: (Profile) -> Path,
    avatars: Path,
    system: FileSystem,
): Boolean {
    if (doomed.deletedAt == null) return false
    // The name comes from a file a person can edit, so one that could lead to another folder deletes nothing.
    val named = doomed.file
    if (named != null && (named.any { it == '/' || it == '\\' } || !named.endsWith(ProfileExtension))) return false
    val database = fileOf(doomed)
    val parent = database.parent ?: return false
    val stem = database.name.substringBeforeLast('.')
    if (stem.isBlank() || stem.all { it == '.' }) return false
    // Some systems take two cases for one file.
    if (others.any { fileOf(it).toString().equals(database.toString(), ignoreCase = true) }) return false

    val path = database.toString()
    val folders =
        listOf(
            downloadFolder(path),
            backupFolder(path),
            coverFolder(path),
            snapshotFolder(path),
            parent / LiveFolder / stem,
        )
    if (folders.any { it.name != stem || it.parent?.parent != parent }) return false

    var clean = true
    fun attempt(what: Path, delete: () -> Unit) {
        runCatching(delete).onFailure {
            clean = false
            AppLog.warn("profile", "could not delete ${what.name}, ${it.message}")
        }
    }
    for (folder in folders) attempt(folder) { system.deleteRecursively(folder) }
    val syncBases = system.listOrNull(parent).orEmpty().filter { it.name.startsWith(database.name + SyncBaseStart) }
    for (file in BesideEndings.map { parent / (database.name + it) } + syncBases) {
        attempt(file) { system.delete(file, mustExist = false) }
    }
    val picture = doomed.avatar?.toPath()
    if (picture != null && picture.parent == avatars && picture.name != ".." &&
        others.none { it.avatar == doomed.avatar }
    ) {
        attempt(picture) { system.delete(picture, mustExist = false) }
    }
    return clean
}
