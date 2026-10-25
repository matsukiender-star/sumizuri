package com.sumizuri.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The accent color the system was set to, such as the colors of the wallpaper on a phone or the accent color of the desktop, or null where the system has none to give.
@Composable
internal expect fun rememberSystemAccent(): Color?
