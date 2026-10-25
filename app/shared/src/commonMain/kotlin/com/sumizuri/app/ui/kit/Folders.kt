package com.sumizuri.app.ui.kit

// Opens a folder in the file manager of the computer. It says whether it could, and on a phone it never can.
expect fun openFolder(path: String): Boolean

// Whether this system has a file manager that can be asked to open a folder.
expect fun canOpenFolders(): Boolean
