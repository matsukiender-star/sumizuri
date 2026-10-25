package com.sumizuri.app.ui.kit.image

import coil3.PlatformContext
import coil3.request.SuccessResult
import coil3.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// How wide the picture of the page the person stopped on is kept.
private const val SnapshotWidth = 480

// Makes a small JPEG of a picture, or null where the system cannot.
internal expect fun encodeSnapshot(image: coil3.Image): ByteArray?

// A small picture of the page the person last stopped on in each title, for the cards that ask them to go on. It is read from the picture cache, which already holds the page, and kept beside the database of the profile, so each profile keeps its own.
object PageSnapshots {
    private var folder: Path? = null

    fun useFolder(path: Path) {
        folder = path
        runCatching { FileSystem.SYSTEM.createDirectories(path) }
    }

    // The picture of a title, or null when none was kept.
    fun of(titleId: Long): String? =
        folder?.let { it / "$titleId.jpg" }?.takeIf { FileSystem.SYSTEM.exists(it) }?.toString()

    suspend fun save(context: PlatformContext, titleId: Long, url: String, headers: Map<String, String>) {
        val target = folder?.let { it / "$titleId.jpg" } ?: return
        val request = buildImageRequest(
            context,
            url,
            headers,
        ).newBuilder().size(Size(SnapshotWidth, SnapshotWidth * 3)).build()
        val result = PageImages.get(context).execute(request) as? SuccessResult ?: return
        val bytes = withContext(Dispatchers.Default) { encodeSnapshot(result.image) } ?: return
        withContext(Dispatchers.IO) { runCatching { FileSystem.SYSTEM.write(target) { write(bytes) } } }
    }
}

// The folder of the pictures of a profile, beside its database and named after it, or in the temporary folder when there is no database file.
fun snapshotFolder(databasePath: String): Path {
    if (databasePath.isBlank()) return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sumizuri-pages"
    val database = databasePath.toPath()
    val parent = database.parent ?: return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sumizuri-pages"
    return parent / "pages" / database.name.substringBeforeLast('.')
}
