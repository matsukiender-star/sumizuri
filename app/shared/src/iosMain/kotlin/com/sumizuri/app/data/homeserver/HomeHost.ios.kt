package com.sumizuri.app.data.homeserver

import okio.Path

// An iPhone is not a server.
actual fun homeHostOf(appFolder: Path): HomeHost? = null
