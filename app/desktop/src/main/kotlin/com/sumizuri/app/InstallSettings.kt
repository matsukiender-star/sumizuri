package com.sumizuri.app

import java.io.File

// The name of the file the Windows installer writes beside the app with what was chosen while installing.
private const val SettingsFile = "sumizuri.ini"

// The choices of the Windows installer, read from the settings file beside the app, which for now set the folders of the library, its settings and its downloads, and without it everything stays in .sumizuri in the home folder.
internal object InstallSettings {
    fun dataFolder(): File {
        val chosen = read()["DataFolder"]?.trim()?.takeIf { it.isNotEmpty() }?.let(::File)
        return (chosen ?: File(System.getProperty("user.home"), ".sumizuri")).apply { mkdirs() }
    }

    // The lines of the file as names and values. The installer writes UTF-8, which may begin with a byte order mark, and lines that start a section or a note are skipped.
    private fun read(): Map<String, String> {
        // The launcher of the installed app names itself, and the file sits beside it.
        val launcher = System.getProperty("jpackage.app-path") ?: return emptyMap()
        val file = File(launcher).parentFile?.resolve(SettingsFile)?.takeIf { it.isFile } ?: return emptyMap()
        return runCatching {
            file
                .readLines(Charsets.UTF_8)
                .asSequence()
                .map { it.removePrefix("").trim() }
                .filter { it.isNotEmpty() && !it.startsWith("[") && !it.startsWith(";") && '=' in it }
                .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
        }.getOrDefault(emptyMap())
    }
}
