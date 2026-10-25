package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import okio.Path

// Whether the system has a way to hand a file to another app. A phone does, through its share sheet. A desktop window has none.
expect fun canShareFiles(): Boolean

// Gives a way to hand a file to another app through the share sheet. The call takes the name the file has there and the file to give. Nothing happens where files cannot be shared.
@Composable
expect fun rememberFileSharer(): (name: String, file: Path) -> Unit
