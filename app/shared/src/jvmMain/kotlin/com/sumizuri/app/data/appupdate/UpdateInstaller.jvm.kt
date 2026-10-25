package com.sumizuri.app.data.appupdate

import okio.FileSystem
import okio.Path
import java.awt.Desktop
import java.io.File

actual object UpdateInstaller {
    // The installer for this system: an MSI or EXE on Windows, a DMG or PKG on a Mac, and a package or AppImage on Linux.
    actual fun pick(assets: List<ReleaseAsset>): ReleaseAsset? {
        val system = System.getProperty("os.name").orEmpty().lowercase()
        val kinds = when {
            "win" in system -> listOf(".msi", ".exe")
            "mac" in system -> listOf(".dmg", ".pkg")
            else -> listOf(".deb", ".rpm", ".appimage", ".tar.gz")
        }
        return kinds.firstNotNullOfOrNull { kind -> assets.firstOrNull { it.name.lowercase().endsWith(kind) } }
    }

    actual fun folder(): Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "sumizuri-updates"

    // Opens the installer. It asks to close the app when it needs to.
    actual fun install(file: String): InstallResult {
        if (!Desktop.isDesktopSupported() ||
            !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
        ) {
            return InstallResult.Failed
        }
        return runCatching {
            Desktop.getDesktop().open(
                File(file),
            )
        }.fold({ InstallResult.Started }, { InstallResult.Failed })
    }
}
