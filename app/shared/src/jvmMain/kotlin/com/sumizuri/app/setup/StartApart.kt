package com.sumizuri.app.setup

import com.sumizuri.app.core.log.AppLog
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

// Starts a program apart from the setup. The launcher of the app runs the app inside a job of Windows that ends everything started from it once the launcher ends, so a program started the usual way would end with the setup: the app opened when the setup closes, or the script that removes the app. Windows Management starts it on its own instead, as the person, and the setup only waits until it has started.
internal object StartApart {
    val powerShell =
        File(System.getenv("SystemRoot") ?: "C:\\Windows", "System32\\WindowsPowerShell\\v1.0\\powershell.exe")

    // The command line of PowerShell running a script file of its own, hidden.
    fun scriptCommand(script: File): String =
        "\"${powerShell.absolutePath}\" -NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File \"${script.absolutePath}\""

    // Starts the command line in the folder given, with its window shown or hidden. False when Windows Management could not start it, and the caller starts it the usual way.
    fun start(command: String, folder: File, hidden: Boolean): Boolean {
        fun quoted(text: String) = "'" + text.replace("'", "''") + "'"
        val script = """
            ${'$'}startup = ([wmiclass]'Win32_ProcessStartup').CreateInstance()
            ${'$'}startup.ShowWindow = ${if (hidden) 0 else 1}
            ${'$'}result = ([wmiclass]'Win32_Process').Create(${quoted(
            command,
        )}, ${quoted(folder.absolutePath)}, ${'$'}startup)
            exit [int]${'$'}result.ReturnValue
        """.trimIndent()
        // The script goes in encoded, so no quote or letter in a folder name can break the command line.
        val encoded = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
        return runCatching {
            val process = ProcessBuilder(
                powerShell.absolutePath,
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-EncodedCommand",
                encoded,
            ).redirectErrorStream(true).start()
            val said = process.inputStream
                .bufferedReader()
                .readText()
                .trim()
            val started = process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0
            if (!started) {
                AppLog.warn(
                    "setup",
                    "Windows Management did not start $command (${runCatching {
                        process.exitValue()
                    }.getOrNull()}): $said",
                )
            }
            started
        }.getOrElse {
            AppLog.warn("setup", "Windows Management could not be asked to start $command: ${it.message}")
            false
        }
    }
}
