package com.sumizuri.app.playback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

actual val touchPlayer: Boolean = true

@Composable
actual fun PlayerSystemUi(keepOn: Boolean) {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context.activity()
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val before = activity?.requestedOrientation
        // The video fills a screen turned sideways, and a swipe from the edge shows the bars for a moment.
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            before?.let { activity.requestedOrientation = it }
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(view, keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
}

// The activity that holds a view, which sits under any number of wrappers.
private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
actual fun rememberPlayerEngine(style: SubtitleStyle): PlayerEngine {
    val context = LocalContext.current
    val engine = remember { ExoEngine(context) }
    LaunchedEffect(engine, style) { engine.setSubtitleStyle(style) }
    DisposableEffect(engine) { onDispose { engine.release() } }
    return engine
}
