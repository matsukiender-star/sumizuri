package com.sumizuri.app.feature.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sumizuri.app.data.model.reader.ReaderOrientation

@Composable
actual fun ReaderSystemUi(hideBars: Boolean, keepOn: Boolean, orientation: ReaderOrientation) {
    val view = LocalView.current
    DisposableEffect(view, orientation) {
        val activity = view.context.activity()
        val before = activity?.requestedOrientation
        // Sensor keeps to the long or the short side but turns with the phone, so the screen does not flip upside down.
        activity?.requestedOrientation = when (orientation) {
            ReaderOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ReaderOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            ReaderOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose { if (activity != null && before != null) activity.requestedOrientation = before }
    }
    DisposableEffect(view, hideBars, keepOn) {
        val controller = view.context
            .activity()
            ?.window
            ?.let { WindowCompat.getInsetsController(it, view) }
        view.keepScreenOn = keepOn
        if (hideBars && controller != null) {
            // A swipe from the edge shows the bars for a moment and they go again by themselves.
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

// The activity that holds a view, which sits under any number of wrappers.
private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
