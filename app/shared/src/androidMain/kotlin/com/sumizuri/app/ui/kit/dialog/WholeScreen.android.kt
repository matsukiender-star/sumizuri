package com.sumizuri.app.ui.kit.dialog

import androidx.compose.ui.window.DialogProperties

actual fun wholeScreenDialog(): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
