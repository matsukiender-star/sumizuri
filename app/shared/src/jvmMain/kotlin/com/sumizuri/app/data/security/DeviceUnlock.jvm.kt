package com.sumizuri.app.data.security

actual object DeviceUnlock {
    actual val usesSystem: Boolean = false

    actual fun available(): Boolean = false

    actual val canHideScreen: Boolean = false

    actual val awayOnBlur: Boolean = true
}
