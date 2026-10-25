package com.sumizuri.app.ui.kit.input

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClip(text: String): ClipEntry = ClipEntry.withPlainText(text)
