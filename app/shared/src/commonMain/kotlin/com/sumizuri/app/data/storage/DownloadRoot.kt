package com.sumizuri.app.data.storage

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.backup.backupFolder
import com.sumizuri.app.data.download.downloadFolder
import com.sumizuri.app.data.work.BusyKind
import com.sumizuri.app.data.work.BusyWork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// The app writes into a folder of its own inside the chosen one, never loose among the person's files.
const val AppFolder = "Sumizuri"

// A copy to another disk is made beside its place under this name and renamed only when whole.
private const val CopyEnding = ".moving"

@Serializable
private class SavedRoot(val folder: String? = null)

enum class MoveProblem { InsideData, Busy, NotEmpty, Failed }

// Where this device keeps downloads, backups and updates of every profile, when the person chose a place. Read once when the app starts, before a profile opens.
object DownloadRoot {
    private val json = Json { ignoreUnknownKeys = true }
    private var file: Path? = null
    private val mutable = MutableStateFlow<Path?>(null)
    val chosen: StateFlow<Path?> = mutable

    fun load(from: Path, files: FileSystem = FileSystem.SYSTEM) {
        file = from
        mutable.value = runCatching {
            json.decodeFromString(SavedRoot.serializer(), files.read(from) { readUtf8() }).folder?.toPath()
        }.getOrNull()
    }

    // The folder the app writes in, or null for beside its data as always.
    fun base(): Path? = mutable.value?.let { it / AppFolder }

    // Moves what every profile saved to the new place, and only then uses it. Anything that fails puts all back where it was. Null goes back beside the data.
    suspend fun change(
        to: Path?,
        databases: List<String>,
        dataFolder: Path,
        files: FileSystem = FileSystem.SYSTEM,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): MoveProblem? = withContext(Dispatchers.IO) {
        val old = base()
        if (to != null &&
            (to.isInside(dataFolder) || old?.let { to.isInside(it) } == true)
        ) {
            return@withContext MoveProblem.InsideData
        }
        if (BusyWork.tasks.value.any {
                it.kind == BusyKind.Downloads || it.kind == BusyKind.Backup
            }
        ) {
            return@withContext MoveProblem.Busy
        }
        val next = to?.let { it / AppFolder }
        val moves = databases
            .flatMap { db ->
                listOf(
                    downloadFolder(db, old) to downloadFolder(db, next),
                    backupFolder(db, old) to backupFolder(db, next),
                )
            }.filter { (from, into) -> from != into && files.exists(from) }
        if (moves.any { (_, into) ->
                files
                    .listOrNull(
                        into,
                    )?.isNotEmpty() == true
            }
        ) {
            return@withContext MoveProblem.NotEmpty
        }
        val total = moves.sumOf { (from, _) -> fileCount(from, files) }
        var count = 0
        val whole = mutableListOf<Pair<Path, Path>>()
        val copies = mutableListOf<Pair<Path, Path>>()
        try {
            BusyWork.keep(BusyKind.MovingDownloads, { count to total }, onStop = {}) {
                for ((from, into) in moves) {
                    files.createDirectories(into.parent!!)
                    files.delete(into, mustExist = false)
                    if (runCatching { files.atomicMove(from, into) }.isSuccess) {
                        whole += from to into
                        count += fileCount(into, files)
                        onProgress(count, total)
                        continue
                    }
                    // Another disk: copied beside its place, then renamed, so a copy is either whole or not there.
                    val part = into.parent!! / (into.name + CopyEnding)
                    files.deleteRecursively(part)
                    copies += from to part
                    copyTree(from, part, files) {
                        count++
                        onProgress(count, total)
                    }
                    files.atomicMove(part, into)
                    copies[copies.lastIndex] = from to into
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            AppLog.warn("storage", "the downloads could not be moved, everything stays where it was: ${error.message}")
            for ((from, into) in whole.asReversed()) runCatching { files.atomicMove(into, from) }
            for ((_, copy) in copies) runCatching { files.deleteRecursively(copy) }
            next?.let { dropIfEmpty(it, files) }
            return@withContext MoveProblem.Failed
        }
        save(to, files)
        for ((from, _) in copies) runCatching { files.deleteRecursively(from) }
        old?.let { dropIfEmpty(it, files) }
        null
    }

    private fun save(to: Path?, files: FileSystem) {
        mutable.value = to
        val target = file ?: return
        val temporary = target.parent!! / (target.name + ".tmp")
        files.write(temporary) { writeUtf8(json.encodeToString(SavedRoot.serializer(), SavedRoot(to?.toString()))) }
        files.atomicMove(temporary, target)
    }
}

// Whether a path is a folder or sits inside it, by whole names, so "Videos2" is not inside "Videos".
private fun Path.isInside(folder: Path): Boolean =
    root == folder.root && segments.take(folder.segments.size) == folder.segments

private fun fileCount(folder: Path, files: FileSystem): Int = files.listRecursively(folder).count {
    files.metadata(it).isRegularFile
}

// Copies a folder file by file and never follows a link out of it.
private fun copyTree(from: Path, into: Path, files: FileSystem, onFile: () -> Unit) {
    files.createDirectories(into)
    for (item in files.list(from)) {
        val meta = files.metadata(item)
        when {
            meta.symlinkTarget != null -> continue

            meta.isDirectory -> copyTree(item, into / item.name, files, onFile)

            else -> {
                files.copy(item, into / item.name)
                onFile()
            }
        }
    }
}

// Removes the folder of the app when only empty folders are left in it.
private fun dropIfEmpty(folder: Path, files: FileSystem) {
    if (!files.exists(folder)) return
    if (files.listRecursively(folder).none {
            files.metadata(it).isRegularFile
        }
    ) {
        runCatching { files.deleteRecursively(folder) }
    }
}
