package com.sumizuri.app.data.work

// A computer does not pause an app in the background, so nothing more is needed while work runs.
actual object BusyPlatform {
    actual fun keepAwake() = Unit
}
