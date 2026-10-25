package com.sumizuri.app.core

// Which build of the app is running, set once at start: a debug build on Android, or a run from Gradle on the desktop. A debug build shows Debug in its name and a red icon, so it is never mistaken for the release.
object AppBuild {
    var isDebug: Boolean = false
}
