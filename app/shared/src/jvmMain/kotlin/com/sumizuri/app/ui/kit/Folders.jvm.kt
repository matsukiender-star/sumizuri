package com.sumizuri.app.ui.kit

import java.awt.Desktop
import java.io.File

actual fun openFolder(path: String): Boolean = runCatching {
    var folder = File(path)
    // A file is shown by its folder.
    if (!folder.isDirectory) folder = folder.parentFile ?: return false
    Desktop.getDesktop().open(folder)
    true
}.getOrDefault(false)

actual fun canOpenFolders(): Boolean =
    Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
