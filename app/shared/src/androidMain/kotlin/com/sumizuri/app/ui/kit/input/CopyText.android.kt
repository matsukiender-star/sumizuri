package com.sumizuri.app.ui.kit.input

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun plainTextClip(text: String): ClipEntry = ClipEntry(ClipData.newPlainText("text", text))
