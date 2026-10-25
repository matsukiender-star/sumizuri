package com.sumizuri.app.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import platform.LocalAuthentication.*
import platform.darwin.*

@Composable
actual fun rememberSystemUnlock(title: String, onUnlocked: () -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onUnlocked)
    return {
        LAContext().evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = title) { success, _ ->
            if (success) dispatch_async(dispatch_get_main_queue()) { latest.value() }
        }
    }
}

// The system gives an app no way to keep itself out of screenshots.
@Composable
actual fun SecureWindow(secure: Boolean) = Unit
