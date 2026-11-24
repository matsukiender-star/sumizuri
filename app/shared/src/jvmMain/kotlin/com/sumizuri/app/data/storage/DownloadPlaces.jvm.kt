package com.sumizuri.app.data.storage

import com.sumizuri.app.core.windows.WindowsFileDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toOkioPath
import java.awt.Frame
import java.awt.KeyboardFocusManager
import java.io.File
import javax.swing.JFileChooser

actual fun removablePlaces(): List<Path>? = null

// The dialog of Windows there, the one of the desktop on Linux when KDE or GNOME left theirs, and the plain one of Java otherwise.
actual suspend fun pickFolder(title: String, start: Path?): Path? {
    val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow as? Frame
    val begin = start?.toFile()?.takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))
    if (WindowsFileDialog.available) return WindowsFileDialog.folder(owner, title, begin)?.toOkioPath()
    val tool = desktopTool(title, begin)
    if (tool != null) {
        return withContext(Dispatchers.IO) {
            val process = ProcessBuilder(tool).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            val answer = process.inputStream
                .bufferedReader()
                .readText()
                .trim()
            // A cancel ends without a folder, and no other dialog follows it.
            answer.takeIf { process.waitFor() == 0 && it.isNotEmpty() }?.let { File(it).toOkioPath() }
        }
    }
    return withContext(Dispatchers.Main) {
        val chooser = JFileChooser(begin).apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.toOkioPath() else null
    }
}

// The command of the folder dialog of KDE or GNOME, run as a list and never through a shell.
private fun desktopTool(title: String, start: File): List<String>? = when {
    onPath("kdialog") -> listOf("kdialog", "--title", title, "--getexistingdirectory", start.path)

    onPath(
        "zenity",
    ) -> listOf("zenity", "--file-selection", "--directory", "--title=$title", "--filename=${start.path}/")

    else -> null
}

private fun onPath(name: String): Boolean = System
    .getenv("PATH")
    .orEmpty()
    .split(File.pathSeparatorChar)
    .any { it.isNotEmpty() && File(it, name).canExecute() }
