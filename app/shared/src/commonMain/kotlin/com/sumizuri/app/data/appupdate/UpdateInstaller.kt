package com.sumizuri.app.data.appupdate

import okio.Path

// A file of a release this system can use, and whether it is the one this system likes best.
data class UpdateFile(val asset: ReleaseAsset, val recommended: Boolean = false)

// What each system does with an update: which files of a release fit it, where the file is saved, and how it is installed.
expect object UpdateInstaller {
    // The files of a release for this device, in the order they are shown, with the one this system likes best marked.
    fun files(assets: List<ReleaseAsset>): List<UpdateFile>

    fun folder(): Path

    // Opens the installer of the system with the file. On a phone that does not let the app install yet, it opens the setting that allows it.
    suspend fun install(file: String): InstallResult
}
