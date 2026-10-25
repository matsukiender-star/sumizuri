package com.sumizuri.app.ui.security

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSystemUnlock(title: String, onUnlocked: () -> Unit): () -> Unit = {}

@Composable
actual fun SecureWindow(secure: Boolean) = Unit
