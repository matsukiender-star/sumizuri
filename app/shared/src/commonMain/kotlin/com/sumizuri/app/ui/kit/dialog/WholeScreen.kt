package com.sumizuri.app.ui.kit.dialog

import androidx.compose.ui.window.DialogProperties

// A dialog over the whole screen, such as a cover shown in full. On a phone it also covers the status bar and the bar of the system at the bottom, so nothing of the page shows at the edges.
expect fun wholeScreenDialog(): DialogProperties
