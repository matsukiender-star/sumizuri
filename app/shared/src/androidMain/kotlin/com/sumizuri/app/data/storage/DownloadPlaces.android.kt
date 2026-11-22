package com.sumizuri.app.data.storage

import android.os.Environment
import com.sumizuri.app.AndroidApp
import okio.Path
import okio.Path.Companion.toOkioPath

// The folders of the app on each card that is in, which need no permission and go away with the app.
actual fun removablePlaces(): List<Path>? = AndroidApp.context
    .getExternalFilesDirs(null)
    .drop(1)
    .filterNotNull()
    .filter { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }
    .map { it.toOkioPath() }

actual suspend fun pickFolder(title: String, start: Path?): Path? = null
