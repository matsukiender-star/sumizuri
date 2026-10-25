package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.sumizuri.app.ui.toByteArray
import com.sumizuri.app.ui.topViewController
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.*

// What a chooser tells: the files the person picked, nothing when they gave up.
private class PickedDelegate(private val onChosen: (List<NSURL>) -> Unit) :
    NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onChosen(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onChosen(emptyList())
    }
}

// The picture the person chose, as the bytes of a JPEG, or null when they gave up.
private class ImageDelegate(private val onImage: (ByteArray?) -> Unit) :
    NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        onImage(image?.let { UIImageJPEGRepresentation(it, 0.92) }?.toByteArray())
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImage(null)
    }
}

// The delegate of a chooser is held weakly by the chooser, so the screen keeps it for as long as the chooser may be open.
private class Held {
    var delegate: Any? = null
}

// Opens the chooser of the system. A file picked is copied into the folder of the app first, so the path given is the app's own.
private fun openChooser(types: List<*>, several: Boolean, held: Held, onChosen: (List<NSURL>) -> Unit) {
    val delegate = PickedDelegate { urls ->
        held.delegate = null
        onChosen(urls)
    }
    held.delegate = delegate
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
    picker.allowsMultipleSelection = several
    picker.delegate = delegate
    topViewController()?.presentViewController(picker, animated = true, completion = null)
}

// The kind is not passed on, as the system knows no type for most of the files asked for here, such as subtitles.
@Composable
@Suppress("UNUSED_PARAMETER")
actual fun rememberFilePicker(kind: FileKind, onPicked: (Path) -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onPicked)
    val held = remember { Held() }
    return {
        openChooser(listOf(UTTypeItem), several = false, held) { urls ->
            urls.firstOrNull()?.path?.let {
                latest.value(it.toPath())
            }
        }
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onPicked)
    val held = remember { Held() }
    return {
        val delegate = ImageDelegate { bytes ->
            held.delegate = null
            if (bytes != null) latest.value(bytes)
        }
        held.delegate = delegate
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
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

// The file is copied under the name wanted, since the chooser saves a file by the name it has.
internal fun copyUnderName(name: String, file: Path): Path {
    val target = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sumizuri-out" / name
    FileSystem.SYSTEM.createDirectories(target.parent!!)
    FileSystem.SYSTEM.copy(file, target)
    return target
}

@Composable
actual fun rememberFileSaver(onDone: (Boolean) -> Unit): (name: String, file: Path) -> Unit {
    val latest = rememberUpdatedState(onDone)
    val held = remember { Held() }
    return { name, file ->
        val target = runCatching { copyUnderName(name, file) }.getOrNull()
        if (target == null) {
            latest.value(false)
        } else {
            val delegate = PickedDelegate { urls ->
                held.delegate = null
                latest.value(urls.isNotEmpty())
            }
            held.delegate = delegate
            val picker =
                UIDocumentPickerViewController(
                    forExportingURLs = listOf(NSURL.fileURLWithPath(target.toString())),
                    asCopy = true,
                )
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberFilesImporter(onPicked: (List<PickedFile>) -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onPicked)
    val held = remember { Held() }
    return {
        openChooser(listOf(UTTypeItem), several = true, held) { urls ->
            val files = urls.mapNotNull { url ->
                url.path?.let {
                    PickedFile(url.lastPathComponent ?: it.toPath().name, it.toPath(), temporary = true)
                }
            }
            if (files.isNotEmpty()) latest.value(files)
        }
    }
}
