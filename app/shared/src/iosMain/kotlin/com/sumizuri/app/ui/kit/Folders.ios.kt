package com.sumizuri.app.ui.kit

// The files of an app are not shown in a file manager on a phone.
actual fun openFolder(path: String): Boolean = false

actual fun canOpenFolders(): Boolean = false
