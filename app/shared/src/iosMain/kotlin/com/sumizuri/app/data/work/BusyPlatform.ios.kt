package com.sumizuri.app.data.work

// The app is not paused at once when it goes to the background, and a longer run on a phone is not planned for here yet, so nothing more is done while work runs.
actual object BusyPlatform {
    actual fun keepAwake() = Unit
}
