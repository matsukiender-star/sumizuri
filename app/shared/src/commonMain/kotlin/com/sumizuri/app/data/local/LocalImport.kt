package com.sumizuri.app.data.local

import com.sumizuri.app.data.text.safeFileName
import okio.FileSystem
import okio.Path

// A file to bring into a local source: its name, where it is now, and whether it is a copy the app made of it, which is moved in rather than copied again.
class LocalImport(val name: String, val path: Path, val temporary: Boolean)

// Brings picked files into the folder of a local source, keeping their names and adding a number when a name is taken. Files the source cannot read are left out, and copies the app made of them are deleted.
fun importLocalFiles(root: Path, sourceId: String, files: List<LocalImport>, fs: FileSystem = FileSystem.SYSTEM) {
    val folder = LocalSources.folderOf(root, sourceId)
    fs.createDirectories(folder)
    val readable = LocalSources.readableFiles(novels = sourceId == LocalSources.NovelId)
    for (file in files) {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        if (ext !in readable) {
            if (file.temporary) runCatching { fs.delete(file.path) }
            continue
        }
        val target =
            folder /
                safeFileName(
                    file.name.substringBeforeLast('.'),
                    ".$ext",
                    fs.list(folder).map { it.name },
                    fallback = "file",
                )
        if (file.temporary) {
            runCatching { fs.atomicMove(file.path, target) }.getOrElse {
                fs.copy(file.path, target)
                fs.delete(file.path)
            }
        } else {
            fs.copy(file.path, target)
        }
    }
}
