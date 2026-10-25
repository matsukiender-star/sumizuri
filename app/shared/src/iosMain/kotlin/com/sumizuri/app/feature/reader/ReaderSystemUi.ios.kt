package com.sumizuri.app.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.sumizuri.app.data.model.reader.ReaderOrientation
import platform.UIKit.*

// The screen is kept on while the reader is open. Hiding the bars and holding the turn of the screen are done by the view controller of the app, which is not written yet.
@Composable
actual fun ReaderSystemUi(hideBars: Boolean, keepOn: Boolean, orientation: ReaderOrientation) {
    DisposableEffect(keepOn) {
        UIApplication.sharedApplication.idleTimerDisabled = keepOn
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}
