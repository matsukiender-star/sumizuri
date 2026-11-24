package com.sumizuri.app.data.storage

import okio.Path

// The cards a phone offers besides its own storage. Null on a computer, where any folder can be picked instead.
expect fun removablePlaces(): List<Path>?

// Asks for a folder on a computer. Null when the person cancels or the system has no way to ask.
expect suspend fun pickFolder(title: String, start: Path?): Path?
