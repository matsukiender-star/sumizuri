package com.sumizuri.app.data.appupdate

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.storage.DownloadRoot
import com.sumizuri.app.setup.StartApart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

private const val Tag = "appupdate"

private val systemName = System.getProperty("os.name").orEmpty().lowercase()

private val onWindows = systemName.startsWith("win")

private val isLinux = "linux" in systemName

actual object UpdateInstaller {
    // The file for this system: an MSI or EXE on Windows, a DMG or PKG on a Mac, and on Linux every format the release brings, with the one this computer likes best marked.
    actual fun files(assets: List<ReleaseAsset>): List<UpdateFile> {
        if (isLinux) return linuxFiles(assets)
        val kinds = if (onWindows) listOf(".msi", ".exe") else listOf(".dmg", ".pkg")
        val chosen = kinds.firstNotNullOfOrNull { kind -> assets.firstOrNull { it.name.lowercase().endsWith(kind) } }
        return listOfNotNull(chosen?.let { UpdateFile(it, true) })
    }

    // In the place the person chose, where an AppImage or a tar.gz outlives a restart, and otherwise the temporary folder. Only this folder of the app is ever emptied.
    actual fun folder(): Path =
        DownloadRoot.base()?.let { it / "updates" } ?: FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sumizuri-updates"

    // Opens the installer, which closes the app when it needs to replace its files. Starting a program apart waits for Windows, so it is done away from the screen.
    actual suspend fun install(file: String): InstallResult = withContext(Dispatchers.IO) {
        val installer = File(file)
        // On Windows the launcher runs the app inside a job that ends everything the app starts. A setup opened the usual way would be in that job, so it could not close the app to replace its files. It is started apart instead.
        if (onWindows &&
            StartApart.start("\"${installer.absolutePath}\"", installer.absoluteFile.parentFile, hidden = false)
        ) {
            AppLog.info(Tag, "started the setup ${installer.name} apart from the app")
            return@withContext InstallResult.Started
        }
        if (isLinux) linuxInstall(installer) else open(installer)
    }
}

// The formats a Linux computer takes from a release, in the order they are shown, with one marked as the one this computer likes best.
private fun linuxFiles(assets: List<ReleaseAsset>): List<UpdateFile> {
    val found = listOf(".deb", ".appimage", ".tar.gz").mapNotNull { kind ->
        assets.firstOrNull { it.name.lowercase().endsWith(kind) }
    }
    if (found.isEmpty()) return emptyList()
    val appImage = found.firstOrNull { it.name.lowercase().endsWith(".appimage") }
    val deb = found.firstOrNull { it.name.lowercase().endsWith(".deb") }
    // An AppImage that runs is replaced in place, a deb when dpkg installed the app, and an AppImage in every other case.
    val preferred = when {
        runningAppImage() != null -> appImage
        installedByDpkg() -> deb
        else -> appImage
    } ?: found.first()
    return found.map { UpdateFile(it, it == preferred) }
}

// The AppImage this app runs from. APPIMAGE is inherited by what an AppImage starts, so it only counts when this Java is inside its APPDIR, or an update would overwrite another app.
private fun runningAppImage(): File? {
    val image = System.getenv("APPIMAGE").orEmpty()
    val mount = System.getenv("APPDIR").orEmpty()
    if (image.isBlank() || mount.isBlank()) return null
    val inside = File(System.getProperty("java.home")).canonicalFile.startsWith(File(mount).canonicalFile)
    return if (inside) File(image) else null
}

// Whether dpkg has the app installed, asked without a shell, which only a Debian computer answers.
private fun installedByDpkg(): Boolean = runCatching {
    val query = ProcessBuilder("dpkg-query", "-W", "-f=\${Status}", "sumizuri").redirectErrorStream(true).start()
    val said = query.inputStream.bufferedReader().readText()
    query.waitFor(3, TimeUnit.SECONDS) && query.exitValue() == 0 && "install ok installed" in said
}.getOrDefault(false)

// What a Linux computer does with the file: an AppImage replaces the one that runs, a deb opens its installer, and anything else is kept for the person to use.
private fun linuxInstall(installer: File): InstallResult {
    if (installer.name.endsWith(".appimage", ignoreCase = true)) {
        val running = runningAppImage()
        return if (running != null) replaceAppImage(installer, running) else keepAppImage(installer)
    }
    if (installer.name.endsWith(".deb", ignoreCase = true)) return open(installer)
    return showFolder(installer)
}

// Copies the new AppImage next to the one that runs, keeps the old as .anterior, and swaps them at once. The running file is never missing, so a failure leaves it as it was.
private fun replaceAppImage(download: File, running: File): InstallResult {
    val folder = running.absoluteFile.parentFile ?: return InstallResult.Failed
    val part = File(folder, running.name + ".part")
    val before = File(folder, running.name + ".anterior")
    return runCatching {
        // A second press would put the new version over the copy of the old one.
        if (Files.mismatch(download.toPath(), running.toPath()) == -1L) return InstallResult.Restart
        download.copyTo(part, overwrite = true)
        if (!part.setExecutable(true)) error("the new AppImage cannot be run")
        // The old one is copied, not moved, so the file that runs stays in place until the swap.
        Files.copy(
            running.toPath(),
            before.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES,
        )
        Files.move(part.toPath(), running.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        AppLog.info(Tag, "replaced ${running.name} with ${download.name}")
        InstallResult.Restart
    }.getOrElse {
        runCatching { part.delete() }
        AppLog.warn(Tag, "could not replace ${running.name}, ${it.message}")
        InstallResult.Failed
    }
}

// Keeps the AppImage where the downloads go, ready to run, and opens its folder.
private fun keepAppImage(download: File): InstallResult {
    if (!download.setExecutable(true)) {
        AppLog.warn(Tag, "the AppImage ${download.name} cannot be run")
        return InstallResult.Failed
    }
    return showFolder(download)
}

// Opens the folder of the file, so the person can take it from there.
private fun showFolder(file: File): InstallResult {
    val folder = file.absoluteFile.parentFile ?: return InstallResult.Failed
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        AppLog.warn(Tag, "this system cannot open ${folder.path}")
        return InstallResult.Failed
    }
    return runCatching { Desktop.getDesktop().open(folder) }.fold(
        {
            AppLog.info(Tag, "opened ${folder.path}")
            InstallResult.Started
        },
        {
            AppLog.warn(Tag, "could not open ${folder.path}, ${it.message}")
            InstallResult.Failed
        },
    )
}

private fun open(installer: File): InstallResult {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        AppLog.warn(Tag, "this system cannot open ${installer.name}")
        return InstallResult.Failed
    }
    return runCatching { Desktop.getDesktop().open(installer) }.fold(
        {
            AppLog.info(Tag, "opened ${installer.name}")
            InstallResult.Started
        },
        {
            AppLog.warn(Tag, "could not open ${installer.name}, ${it.message}")
            InstallResult.Failed
        },
    )
}
