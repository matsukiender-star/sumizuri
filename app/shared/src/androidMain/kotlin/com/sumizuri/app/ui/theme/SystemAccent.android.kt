package com.sumizuri.app.ui.theme

import android.os.Build
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// The main color Android takes from the wallpaper, from Android 12 on.
@Composable
internal actual fun rememberSystemAccent(): Color? {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context).primary else null
    }
}
