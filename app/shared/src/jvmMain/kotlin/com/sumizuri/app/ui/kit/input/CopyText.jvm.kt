package com.sumizuri.app.ui.kit.input

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClip(text: String): ClipEntry = ClipEntry(StringSelection(text))
