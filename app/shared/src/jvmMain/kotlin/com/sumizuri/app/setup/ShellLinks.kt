package com.sumizuri.app.setup

import com.sumizuri.app.core.windows.Com
import com.sun.jna.Memory
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference
import java.io.File

// Makes and reads shortcuts the way Windows itself does, through its ShellLink object in its Unicode form, so folders in any language work. The places of each call in the tables of the objects are fixed by Windows.
internal object ShellLinks {
    // The places in the table of IShellLinkW, and of Load and Save in that of IPersistFile.
    private const val GetPath = 3
    private const val SetDescription = 7
    private const val SetWorkingDirectory = 9
    private const val SetIconLocation = 17
    private const val SetPath = 20
    private const val Load = 5
    private const val Save = 6

    // The longest path a shortcut holds, in letters, and the flag that asks for it as it was written.
    private const val MaxPath = 32_768
    private const val RawPath = 0x4

    private val ShellLink = Com.guid("00021401-0000-0000-C000-000000000046")
    private val IShellLinkW = Com.guid("000214F9-0000-0000-C000-000000000046")
    private val IPersistFile = Com.guid("0000010B-0000-0000-C000-000000000046")

    // Makes a shortcut at the file given that starts the app given in its own folder, with its icon.
    fun make(link: File, target: File, description: String) = Com.on {
        val shortcut = checkNotNull(Com.create(ShellLink, IShellLinkW)) { "Windows could not make a shortcut" }
        try {
            Com.call(shortcut, SetPath, WString(target.absolutePath))
            Com.call(shortcut, SetWorkingDirectory, WString(target.parentFile.absolutePath))
            Com.call(shortcut, SetIconLocation, WString(target.absolutePath), 0)
            Com.call(shortcut, SetDescription, WString(description))
            val persist = PointerByReference()
            check(
                Com.call(shortcut, Com.QueryInterface, IPersistFile, persist) == 0,
            ) { "Windows could not save a shortcut" }
            try {
                link.parentFile.mkdirs()
                val saved = Com.call(persist.value, Save, WString(link.absolutePath), 1)
                check(
                    saved == 0,
                ) { "the shortcut ${link.absolutePath} could not be saved (${Integer.toHexString(saved)})" }
            } finally {
                Com.call(persist.value, Com.Release)
            }
        } finally {
            Com.call(shortcut, Com.Release)
        }
    }

    // The program a shortcut starts, or null when it cannot be read.
    fun target(link: File): File? = Com.on {
        val shortcut = Com.create(ShellLink, IShellLinkW) ?: return@on null
        try {
            val persist = PointerByReference()
            if (Com.call(shortcut, Com.QueryInterface, IPersistFile, persist) != 0) return@on null
            try {
                if (Com.call(persist.value, Load, WString(link.absolutePath), 0) != 0) return@on null
            } finally {
                Com.call(persist.value, Com.Release)
            }
            val path = Memory(MaxPath * 2L).apply { clear() }
            if (Com.call(shortcut, GetPath, path, MaxPath, null, RawPath) != 0) return@on null
            path.getWideString(0).takeIf { it.isNotBlank() }?.let(::File)
        } finally {
            Com.call(shortcut, Com.Release)
        }
    }
}
