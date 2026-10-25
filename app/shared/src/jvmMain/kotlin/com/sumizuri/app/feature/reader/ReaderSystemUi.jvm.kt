package com.sumizuri.app.feature.reader

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.reader.ReaderOrientation

// A desktop window has no system bars to hide and no orientation to hold, and the computer is left to its own sleep.
@Composable
actual fun ReaderSystemUi(hideBars: Boolean, keepOn: Boolean, orientation: ReaderOrientation) = Unit
