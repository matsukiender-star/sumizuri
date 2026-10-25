@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.data.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.LocalAuthentication.*

// The lock of the phone, which is Face ID, Touch ID or the passcode. The system tells the app itself when it goes to the background, and cannot hide it from screenshots.
actual object DeviceUnlock {
    actual val usesSystem: Boolean = true

    actual fun available(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)

    actual val canHideScreen: Boolean = false

    actual val awayOnBlur: Boolean = false
}
