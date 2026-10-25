package com.sumizuri.app.ui

import androidx.compose.runtime.Composable

// On Android, keeps the window on the fastest refresh the screen allows so Compose UI can run at 120 Hz instead of staying at 90 Hz.
@Composable
internal expect fun UiRefreshRate()
