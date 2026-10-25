package com.sumizuri.app.ui.kit.input

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.ByteArrayOutputStream
import java.io.File

// The kind is not passed on, as Android knows no type for most of the files asked for here, such as subtitles.
@Composable
@Suppress("UNUSED_PARAMETER")
actual fun rememberFilePicker(kind: FileKind, onPicked: (Path) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val copy =
                withContext(Dispatchers.IO) { context.copyPicked(uri, File(context.cacheDir, "picked-file.tmp")) }
            if (copy != null) latest.value(copy.toOkioPath())
        }
    }
    return { launcher.launch("*/*") }
}

@Composable
actual fun rememberFileSaver(onDone: (Boolean) -> Unit): (name: String, file: Path) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onDone)
    // The file waits here while the person picks where it goes.
    val waiting = remember { mutableStateOf<Path?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            val file = waiting.value
            waiting.value = null
            if (uri == null || file == null) {
                latest.value(false)
                return@rememberLauncherForActivityResult
            }
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver
                            .openOutputStream(
                                uri,
                            )?.use { out ->
                                File(file.toString()).inputStream().use {
                                    it.copyTo(out)
                                }
                            } !=
                            null
                    }.getOrDefault(false)
                }
                latest.value(saved)
            }
        }
    return { name, file ->
        waiting.value = file
        launcher.launch(name)
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val resolver = LocalContext.current.contentResolver
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val bytes = uri?.let {
            runCatching {
                resolver
                    .openInputStream(
                        it,
                    )?.use { stream -> stream.readBytes() }
            }.getOrNull()
        }
        if (bytes != null) onPicked(bytes)
    }
    return { launcher.launch("image/*") }
}

actual fun squareAvatar(bytes: ByteArray, size: Int): ByteArray? {
    val picture = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull() ?: return null
    val side = minOf(picture.width, picture.height)
    val square = Bitmap.createBitmap(picture, (picture.width - side) / 2, (picture.height - side) / 2, side, side)
    val scaled = Bitmap.createScaledBitmap(square, size, size, true)
    return ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
}

@Composable
actual fun rememberFilesImporter(onPicked: (List<PickedFile>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val copies = withContext(Dispatchers.IO) {
                val folder = File(context.cacheDir, "imported").apply { mkdirs() }
                uris.mapIndexedNotNull { index, uri ->
                    runCatching {
                        // The name the file has on the device, which the system only gives through a query.
                        val name =
                            context.contentResolver
                                .query(
                                    uri,
                                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                                    null,
                                    null,
                                    null,
                                )?.use { cursor ->
                                    if (cursor.moveToFirst()) cursor.getString(0) else null
                                } ?: "file-$index"
                        val target = context.copyPicked(uri, File(folder, "$index-$name")) ?: return@runCatching null
                        PickedFile(name, target.toOkioPath(), temporary = true)
                    }.getOrNull()
                }
            }
            if (copies.isNotEmpty()) latest.value(copies)
        }
    }
    return { launcher.launch("*/*") }
}

// Copies a picked file into a file of the app, as the picker only gives a way to read it. Null when it cannot be read.
private fun Context.copyPicked(uri: Uri, target: File): File? = runCatching {
    contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } } ?: return null
    target
}.getOrNull()
