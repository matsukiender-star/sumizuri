package com.sumizuri.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// An iPhone gives apps no accent color of its own.
@Composable
internal actual fun rememberSystemAccent(): Color? = null
