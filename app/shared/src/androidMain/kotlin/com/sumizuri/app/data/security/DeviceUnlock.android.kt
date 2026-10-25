package com.sumizuri.app.data.security

import android.app.KeyguardManager
import com.sumizuri.app.AndroidApp

actual object DeviceUnlock {
    actual val usesSystem: Boolean = true

    // A PIN, pattern, password or fingerprint is set on the device.
    actual fun available(): Boolean =
        AndroidApp.context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    actual val canHideScreen: Boolean = true

    actual val awayOnBlur: Boolean = false
}
