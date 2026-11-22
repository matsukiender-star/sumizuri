package com.sumizuri.app.data.homeserver

import okio.Path

// A phone joins a computer and is not a server itself.
actual fun homeHostOf(appFolder: Path): HomeHost? = null
