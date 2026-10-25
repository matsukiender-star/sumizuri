package com.sumizuri.app.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.openZip

// Reading ZIP archives, such as CBZ and EPUB files. What is in the last few archives is kept, so a reader turning pages does not read the list of an archive again for each page. An archive that changed on the device is read again.
private class OpenArchive(val path: String, val stamp: Pair<Long?, Long?>, val files: FileSystem)

private const val KeptArchives = 4
private val Root = "/".toPath()
private val kept = MutableStateFlow(emptyList<OpenArchive>())

private fun open(archive: String): FileSystem {
    val path = archive.toPath()
    val stamp = FileSystem.SYSTEM.metadataOrNull(path).let { it?.lastModifiedAtMillis to it?.size }
    kept.value.firstOrNull { it.path == archive && it.stamp == stamp }?.let { return it.files }
    val opened = OpenArchive(archive, stamp, FileSystem.SYSTEM.openZip(path))
    kept.update { list -> (listOf(opened) + list.filter { it.path != archive }).take(KeptArchives) }
    return opened.files
}

private fun entryPath(entry: String): Path = Root / entry

// The names of the files inside an archive. Folders are left out.
fun archiveEntries(archive: String): List<String> {
    val zip = open(archive)
    return zip
        .listRecursively(Root)
        .filter { zip.metadata(it).isRegularFile }
        .map { it.toString().removePrefix("/") }
        .toList()
}

// The bytes of one file inside an archive.
fun archiveEntry(archive: String, entry: String): ByteArray = open(archive).read(entryPath(entry)) { readByteArray() }

// One file inside an archive to read as it goes, for a picture that is decoded straight from it.
fun archiveSource(archive: String, entry: String): Source = open(archive).source(entryPath(entry))
