package com.sumizuri.app.feature.reader

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.reader.ReaderOrientation

// Keeps the screen on, hides the system bars of the phone and holds its orientation while the reader is open. A desktop window has nothing to do.
@Composable
expect fun ReaderSystemUi(hideBars: Boolean, keepOn: Boolean, orientation: ReaderOrientation)
