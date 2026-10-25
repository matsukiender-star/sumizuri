@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.data.account

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

// The Keychain is not used yet, so nothing is sealed and the file is only kept private. The folder of an app is closed to every other app, and the system locks the file with the passcode of the phone.
actual object DeviceSecret {
    actual val canSeal: Boolean = false

    actual fun seal(bytes: ByteArray): ByteArray? = null

    actual fun open(bytes: ByteArray): ByteArray? = null

    actual fun keepPrivate(file: String) {
        NSFileManager.defaultManager.setAttributes(
            mapOf(NSFileProtectionKey to NSFileProtectionComplete),
            ofItemAtPath = file,
            error = null,
        )
    }
}
