package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import okio.Path

// What kind of file a chooser offers first. Every kind can still be picked from it.
enum class FileKind { Any, Subtitles }

// Gives a way to ask the person for a file from their device, of the kind given first. The call opens the chooser, and the path of the file comes to the callback. On a phone the file is copied to the cache first, since the system gives a stream and not a path.
@Composable
expect fun rememberFilePicker(kind: FileKind = FileKind.Any, onPicked: (Path) -> Unit): () -> Unit

// Gives a way to ask the person for a picture from their device. The call opens the chooser, and the bytes of the chosen file come to the callback.
@Composable
expect fun rememberImagePicker(onPicked: (ByteArray) -> Unit): () -> Unit

// Cuts a picture to a square from the middle, scales it to the size and gives it as a PNG, or null when the bytes are not a picture.
expect fun squareAvatar(bytes: ByteArray, size: Int): ByteArray?

// Gives a way to save a file the app made somewhere the person picks. The call takes the name to suggest and the file to copy, and the callback says whether it was saved.
@Composable
expect fun rememberFileSaver(onDone: (Boolean) -> Unit): (name: String, file: Path) -> Unit

// A file the person picked, with the name it had on their device, ready to be read at the path. A temporary one is a copy the app made and deletes once it is used.
data class PickedFile(val name: String, val path: Path, val temporary: Boolean = false)

// Gives a way to ask the person for several files at once, keeping their names. On a phone each is copied to the cache first, and the copies are the app's to delete.
@Composable
expect fun rememberFilesImporter(onPicked: (List<PickedFile>) -> Unit): () -> Unit
