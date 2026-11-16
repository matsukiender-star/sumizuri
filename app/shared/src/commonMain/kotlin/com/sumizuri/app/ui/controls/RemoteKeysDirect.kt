package com.sumizuri.app.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

// The screens that take the keys of a TV remote themselves, such as the player and the reader. While any of them shows, the pointer of the remote keeps out of the way.
object RemoteKeysDirect {
    private var count = 0
    val active: Boolean get() = count > 0

    // Holds the keys of the remote for as long as the screen that calls it shows.
    @Composable
    fun Take() {
        DisposableEffect(Unit) {
            count++
            onDispose { count-- }
        }
    }
}
