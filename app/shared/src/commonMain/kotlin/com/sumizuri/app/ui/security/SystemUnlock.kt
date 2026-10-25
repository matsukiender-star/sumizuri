package com.sumizuri.app.ui.security

import androidx.compose.runtime.Composable

// Gives a way to ask for the lock of the system, such as a fingerprint or the PIN of the phone, which calls onUnlocked once it is passed. Where the system has no lock the app can ask for, it does nothing.
@Composable
expect fun rememberSystemUnlock(title: String, onUnlocked: () -> Unit): () -> Unit

// Hides the app from screenshots and from the list of recent apps while secure is true, where the system can.
@Composable
expect fun SecureWindow(secure: Boolean)
