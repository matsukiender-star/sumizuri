package com.sumizuri.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

// Asks the screen for its fastest refresh at the current size. Without this, many phones stay at 90 Hz even when 120 Hz is available.
fun Activity.requestMaxRefreshRate() {
    val display = windowDisplay() ?: return
    val params = window.attributes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val best = bestMode(display) ?: return
        if (params.preferredDisplayModeId == best.modeId && params.preferredRefreshRate == best.refreshRate) return
        window.attributes =
            params.apply {
                preferredDisplayModeId = best.modeId
                preferredRefreshRate = best.refreshRate
            }
    } else {
        val peak = display.supportedRefreshRates.maxOrNull() ?: return
        if (params.preferredRefreshRate == peak) return
        window.attributes = params.apply { preferredRefreshRate = peak }
    }
}

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Activity.windowDisplay(): Display? =
    getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)

private fun bestMode(display: Display): Display.Mode? {
    val modes = display.supportedModes
    if (modes.isEmpty()) return null
    val current = display.mode
    return modes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate }
        ?: modes.maxByOrNull { it.refreshRate }
}
