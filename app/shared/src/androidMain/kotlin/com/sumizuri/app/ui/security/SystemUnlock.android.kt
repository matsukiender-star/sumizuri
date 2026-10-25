package com.sumizuri.app.ui.security

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.sumizuri.app.ui.findActivity

@Composable
actual fun rememberSystemUnlock(title: String, onUnlocked: () -> Unit): () -> Unit {
    val activity = LocalContext.current.findActivity()
    val latest = rememberUpdatedState(onUnlocked)
    // Before Android 11 the prompt of the system cannot take the PIN of the phone, so the screen of the lock is opened instead.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) latest.value()
    }
    return remember(activity, title) {
        {
            if (activity != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val prompt = BiometricPrompt
                        .Builder(activity)
                        .setTitle(title)
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                        ).build()
                    prompt.authenticate(
                        CancellationSignal(),
                        activity.mainExecutor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                                latest.value()
                        },
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val intent = activity
                        .getSystemService(
                            KeyguardManager::class.java,
                        )?.createConfirmDeviceCredentialIntent(title, null)
                    // No lock is set on the device, so there is nothing to pass.
                    if (intent == null) latest.value() else launcher.launch(intent)
                }
            }
        }
    }
}

@Composable
actual fun SecureWindow(secure: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, secure) {
        val window = activity?.window
        if (secure) {
            window?.addFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { if (secure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
