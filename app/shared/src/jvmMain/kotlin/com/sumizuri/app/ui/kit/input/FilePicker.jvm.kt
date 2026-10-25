package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.sumizuri.app.core.windows.FileType
import com.sumizuri.app.core.windows.WindowsFileDialog
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.file_kind_pictures
import com.sumizuri.app.shared.resources.file_kind_subtitles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.awt.FileDialog
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.File

private val Pictures = listOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
private val Subtitles = listOf("srt", "ass", "ssa", "vtt", "sub")

// The window the person is in, so the dialog stays over it and keeps it waiting.
private fun activeWindow(): Frame? = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as? Frame

// The kinds of file a chooser offers first, with the words for them.
@Composable
private fun typesOf(kind: FileKind): List<FileType> = when (kind) {
    FileKind.Any -> emptyList()
    FileKind.Subtitles -> listOf(FileType(stringResource(Res.string.file_kind_subtitles), Subtitles))
}

// Asks for files with the dialog of Windows where there is one, and with the plain one of Java elsewhere or when it could not open. Empty when the person cancels.
private suspend fun pickFiles(types: List<FileType>, several: Boolean): List<File> {
    val owner = activeWindow()
    if (WindowsFileDialog.available) WindowsFileDialog.open(owner, types, several)?.let { return it }
    return withContext(Dispatchers.IO) {
        val dialog = FileDialog(owner, "", FileDialog.LOAD)
        val endings = types.flatMap { it.extensions }.toSet()
        if (endings.isNotEmpty()) {
            dialog.setFilenameFilter { _, name ->
                name.substringAfterLast('.', "").lowercase() in
                    endings
            }
        }
        dialog.isMultipleMode = several
        dialog.isVisible = true
        dialog.files.orEmpty().toList()
    }
}

@Composable
actual fun rememberFilePicker(kind: FileKind, onPicked: (Path) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val types = typesOf(kind)
    return {
        scope.launch { pickFiles(types, several = false).firstOrNull()?.let { latest.value(it.toOkioPath()) } }
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val types = listOf(FileType(stringResource(Res.string.file_kind_pictures), Pictures))
    return {
        scope.launch {
            val file = pickFiles(types, several = false).firstOrNull() ?: return@launch
            val bytes = withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull() }
            if (bytes != null) latest.value(bytes)
        }
    }
}

actual fun squareAvatar(bytes: ByteArray, size: Int): ByteArray? {
    val picture = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
    val side = minOf(picture.width, picture.height).toFloat()
    val from = Rect.makeXYWH((picture.width - side) / 2f, (picture.height - side) / 2f, side, side)
    val surface = Surface.makeRaster(ImageInfo.makeN32(size, size, ColorAlphaType.PREMUL))
    surface.canvas.drawImageRect(
        picture,
        from,
        Rect.makeWH(size.toFloat(), size.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)?.bytes
}

@Composable
actual fun rememberFileSaver(onDone: (Boolean) -> Unit): (name: String, file: Path) -> Unit {
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onDone)
    return { name, file ->
        scope.launch {
            val owner = activeWindow()
            // The dialog of Windows, or the plain one of Java elsewhere. Null when the person cancels.
            val chosen = if (WindowsFileDialog.available) {
                WindowsFileDialog.save(owner, name)
            } else {
                withContext(Dispatchers.IO) {
                    val dialog = FileDialog(owner, "", FileDialog.SAVE)
                    dialog.file = name
                    dialog.isVisible = true
                    dialog.file?.let { File(dialog.directory, it) }
                }
            }
            val saved =
                chosen != null &&
                    withContext(
                        Dispatchers.IO,
                    ) { runCatching { File(file.toString()).copyTo(chosen, overwrite = true) }.isSuccess }
            latest.value(saved)
        }
    }
}

@Composable
actual fun rememberFilesImporter(onPicked: (List<PickedFile>) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    return {
        scope.launch {
            val files = pickFiles(emptyList(), several = true).map { PickedFile(it.name, it.toOkioPath()) }
            if (files.isNotEmpty()) latest.value(files)
        }
    }
}
