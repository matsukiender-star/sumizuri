package com.sumizuri.app.setup

import com.sumizuri.app.core.log.AppLog
import java.io.File
import java.util.concurrent.TimeUnit

// The key under the classes of the person that tells Windows to open sumizuri links with the app.
internal const val LinkKey = "Software\\Classes\\sumizuri"

// The entry that tells a Linux desktop to open sumizuri links with the app. It is hidden from the menu of apps, which keeps the entry of the package.
private const val LinuxEntry = "sumizuri-links.desktop"

// Tells Windows that sumizuri links, such as an Open in Sumizuri button on the page of a repo, open with this copy of the app.
internal fun registerAppLinks(exe: File) {
    WinRegistry.write(LinkKey, texts = mapOf("" to "URL:Sumizuri", "URL Protocol" to ""), numbers = emptyMap())
    WinRegistry.write("$LinkKey\\DefaultIcon", texts = mapOf("" to "\"${exe.absolutePath}\",0"), numbers = emptyMap())
    WinRegistry.write(
        "$LinkKey\\shell\\open\\command",
        texts = mapOf("" to "\"${exe.absolutePath}\" \"%1\""),
        numbers = emptyMap(),
    )
}

// Run at each start of an installed app, so the links follow the copy that runs, also after it moved or was updated. A run from the build leaves them alone.
fun refreshAppLinks() {
    val launcher = System.getProperty("jpackage.app-path")?.let(::File) ?: return
    val system = System.getProperty("os.name").orEmpty()
    when {
        system.startsWith("Windows") -> refreshWindows(launcher)
        system.startsWith("Linux") -> refreshLinux(launcher)
    }
}

private fun refreshWindows(launcher: File) {
    val folder = launcher.parentFile ?: return
    if (!WindowsSetup.registeredAt(folder)) return
    val wanted = "\"${launcher.absolutePath}\" \"%1\""
    if (WinRegistry.text("$LinkKey\\shell\\open\\command", "") == wanted) return
    runCatching {
        registerAppLinks(launcher)
    }.onFailure { AppLog.warn("setup", "the sumizuri links could not be registered: ${it.message}") }
}

// Writes the entry of the links in the folder of apps of the person, the way the desktop standard asks, and makes it the one for sumizuri links. The package or the unpacked folder both work, as the entry names the launcher that runs now.
private fun refreshLinux(launcher: File) {
    val data =
        System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(System.getProperty("user.home"), ".local/share")
    val folder = File(data, "applications")
    val entry = File(folder, LinuxEntry)
    val icon = launcher.parentFile
        ?.parentFile
        ?.resolve("lib")
        ?.listFiles { file -> file.extension == "png" }
        ?.firstOrNull()
    val wanted = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=Sumizuri")
        appendLine("Exec=\"${launcher.absolutePath.replace("\"", "\\\"")}\" %u")
        icon?.let { appendLine("Icon=${it.absolutePath}") }
        appendLine("NoDisplay=true")
        appendLine("Terminal=false")
        appendLine("MimeType=x-scheme-handler/sumizuri;")
    }
    if (entry.isFile && entry.readText() == wanted) return
    runCatching {
        folder.mkdirs()
        entry.writeText(wanted)
        // xdg-mime makes the entry the one for the links. A desktop without it reads the entry by its type the next time it looks.
        run("xdg-mime", "default", LinuxEntry, "x-scheme-handler/sumizuri")
        run("update-desktop-database", folder.absolutePath)
        AppLog.info("setup", "sumizuri links open with ${launcher.absolutePath}")
    }.onFailure { AppLog.warn("setup", "the sumizuri links could not be registered: ${it.message}") }
}

// Runs a tool of the desktop and waits a little for it. A tool that is not there is passed over.
private fun run(vararg command: String) {
    runCatching { ProcessBuilder(*command).redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS) }
}
