package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import okio.Path

actual fun canShareFiles(): Boolean = false

@Composable
actual fun rememberFileSharer(): (name: String, file: Path) -> Unit = { _, _ -> }
