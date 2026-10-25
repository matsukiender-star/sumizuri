package com.sumizuri.app.core.windows

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.Window
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.resume

@Suppress("FunctionName")
private interface Shell32 : Library {
    fun SHCreateItemFromParsingName(path: WString, bind: Pointer?, iid: Pointer, result: PointerByReference): Int
}

// A kind of file the dialog offers, such as pictures, with the endings of its files.
data class FileType(val label: String, val extensions: List<String>)

// The file dialog of Windows itself, the one Explorer looks like, with its places at the side, its search and the light or dark of Windows: for opening one file or several, for saving a file, and for picking a folder. The places of each call in the tables of the objects are fixed by Windows.
object WindowsFileDialog {
    private const val OverwritePrompt = 0x2
    private const val PickFolders = 0x20
    private const val FileSystemOnly = 0x40
    private const val ManySelected = 0x200
    private const val PathMustExist = 0x800
    private const val FileMustExist = 0x1000
    private const val FileSystemPath = 0x80058000.toInt()

    // The places in the table of IFileDialog, which both kinds of dialog share, then those only the open dialog has, then those of IShellItemArray and IShellItem.
    private const val Show = 3
    private const val SetFileTypes = 4
    private const val SetFileTypeIndex = 5
    private const val SetOptions = 9
    private const val GetOptions = 10
    private const val SetFolder = 12
    private const val SetFileName = 15
    private const val SetTitle = 17
    private const val GetResult = 20
    private const val SetDefaultExtension = 22
    private const val GetResults = 27
    private const val GetCount = 7
    private const val GetItemAt = 8
    private const val GetDisplayName = 5

    private val FileOpenDialog = Com.guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7")
    private val IFileOpenDialog = Com.guid("D57C7288-D4AD-4768-BE02-9D969532D960")
    private val FileSaveDialog = Com.guid("C0B4E2F3-BA21-4773-8DBA-335EC946EB8B")
    private val IFileSaveDialog = Com.guid("84BCCD23-5FDE-4CDB-AEA4-AF64B83D78AB")
    private val IShellItem = Com.guid("43826D1E-E718-42EE-BC55-A1E261C37BFE")

    private val shell: Shell32 by lazy { Native.load("shell32", Shell32::class.java) }

    // Whether the dialog of Windows can be used here, which is on Windows only.
    val available: Boolean = System
        .getProperty("os.name")
        .orEmpty()
        .lowercase()
        .contains("win")

    // Asks for one file, or several when asked, of the kinds given, with every file as the last kind. An empty list when the person cancels, and null when the dialog could not open.
    suspend fun open(
        owner: Window?,
        types: List<FileType> = emptyList(),
        several: Boolean = false,
        title: String? = null,
    ): List<File>? = onComThread { openNow(owner, types, several, title, folders = false, start = null) }

    // Asks for a folder, starting in the one given. Null when the person cancels or the dialog could not open.
    suspend fun folder(owner: Window?, title: String? = null, start: File? = null): File? = onComThread {
        openNow(owner, emptyList(), several = false, title, folders = true, start = start)
    }?.firstOrNull()

    // Asks where to save a file, suggesting the name given and its kind. Null when the person cancels or the dialog could not open.
    suspend fun save(owner: Window?, name: String, title: String? = null): File? =
        onComThread { saveNow(owner, name, title) }

    // The dialog wants a thread of its own that is set up for COM, and it holds that thread until it closes.
    private suspend fun <T> onComThread(work: () -> T?): T? = suspendCancellableCoroutine { done ->
        thread(name = "file dialog", isDaemon = true) {
            done.resume(runCatching { Com.on(work) }.getOrNull())
        }
    }

    private fun openNow(
        owner: Window?,
        types: List<FileType>,
        several: Boolean,
        title: String?,
        folders: Boolean,
        start: File?,
    ): List<File>? {
        val dialog = Com.create(FileOpenDialog, IFileOpenDialog) ?: return null
        try {
            var options = FileSystemOnly or PathMustExist
            options = options or if (folders) PickFolders else FileMustExist
            if (several) options = options or ManySelected
            prepare(dialog, options, title, types)
            start?.takeIf { it.isDirectory }?.let { setFolder(dialog, it) }
            if (Com.call(dialog, Show, windowOf(owner)) != 0) return emptyList()
            if (!several) return listOfNotNull(result(dialog))
            val array = PointerByReference()
            if (Com.call(dialog, GetResults, array) != 0) return emptyList()
            try {
                val count = Memory(4)
                Com.call(array.value, GetCount, count)
                return (0 until count.getInt(0)).mapNotNull { index ->
                    val item = PointerByReference()
                    if (Com.call(array.value, GetItemAt, index, item) != 0) null else pathOf(item.value)
                }
            } finally {
                Com.call(array.value, Com.Release)
            }
        } finally {
            Com.call(dialog, Com.Release)
        }
    }

    private fun saveNow(owner: Window?, name: String, title: String?): File? {
        val dialog = Com.create(FileSaveDialog, IFileSaveDialog) ?: return null
        try {
            val ending = name.substringAfterLast('.', "")
            // The kind of the suggested file comes first, so the name keeps its ending.
            val types = if (ending.isEmpty()) emptyList() else listOf(FileType(ending.uppercase(), listOf(ending)))
            prepare(dialog, FileSystemOnly or PathMustExist or OverwritePrompt, title, types)
            Com.call(dialog, SetFileName, WString(name))
            if (ending.isNotEmpty()) Com.call(dialog, SetDefaultExtension, WString(ending))
            if (Com.call(dialog, Show, windowOf(owner)) != 0) return null
            return result(dialog)
        } finally {
            Com.call(dialog, Com.Release)
        }
    }

    // The options, the title and the kinds of file, the last kind always every file.
    private fun prepare(dialog: Pointer, options: Int, title: String?, types: List<FileType>) {
        val now = Memory(4)
        Com.call(dialog, GetOptions, now)
        Com.call(dialog, SetOptions, now.getInt(0) or options)
        title?.let { Com.call(dialog, SetTitle, WString(it)) }
        if (types.isEmpty()) return
        val all = types + FileType("*", listOf("*"))
        // Each kind is two pointers, its name and its pattern, and the strings must live until the call is done.
        val strings = all.flatMap { type ->
            val pattern = type.extensions.joinToString(";") { "*.$it" }
            listOf(wide(if (type.label == "*") "*.*" else "${type.label} ($pattern)"), wide(pattern))
        }
        val table = Memory(strings.size.toLong() * Native.POINTER_SIZE)
        strings.forEachIndexed { index, text -> table.setPointer(index.toLong() * Native.POINTER_SIZE, text) }
        Com.call(dialog, SetFileTypes, all.size, table)
        Com.call(dialog, SetFileTypeIndex, 1)
    }

    private fun wide(text: String): Memory = Memory((text.length + 1) * 2L).apply { setWideString(0, text) }

    private fun setFolder(dialog: Pointer, folder: File) {
        val item = PointerByReference()
        if (shell.SHCreateItemFromParsingName(WString(folder.absolutePath), null, IShellItem, item) == 0) {
            Com.call(dialog, SetFolder, item.value)
            Com.call(item.value, Com.Release)
        }
    }

    private fun result(dialog: Pointer): File? {
        val item = PointerByReference()
        if (Com.call(dialog, GetResult, item) != 0) return null
        return pathOf(item.value)
    }

    // The path of a shell item, which it lets go of after.
    private fun pathOf(item: Pointer): File? = try {
        val name = PointerByReference()
        if (Com.call(item, GetDisplayName, FileSystemPath, name) != 0) {
            null
        } else {
            File(name.value.getWideString(0)).also { Com.free(name.value) }
        }
    } finally {
        Com.call(item, Com.Release)
    }

    private fun windowOf(owner: Window?): Pointer? = owner
        ?.takeIf {
            it.isDisplayable
        }?.let { Native.getComponentPointer(it) }
}
