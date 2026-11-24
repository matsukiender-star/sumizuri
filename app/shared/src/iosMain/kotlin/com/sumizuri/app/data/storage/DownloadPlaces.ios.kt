package com.sumizuri.app.data.storage

import okio.Path

actual fun removablePlaces(): List<Path>? = emptyList()

actual suspend fun pickFolder(title: String, start: Path?): Path? = null
