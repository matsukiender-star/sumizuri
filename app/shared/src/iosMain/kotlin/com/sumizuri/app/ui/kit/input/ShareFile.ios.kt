@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.ui.kit.input

import androidx.compose.runtime.Composable
import com.sumizuri.app.ui.topViewController
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import platform.Foundation.*
import platform.UIKit.*

actual fun canShareFiles(): Boolean = true

// The share sheet of the system, with the file under the name it should have there.
@Composable
actual fun rememberFileSharer(): (name: String, file: Path) -> Unit = { name, file ->
    val target = runCatching { copyUnderName(name, file) }.getOrNull()
    val top = topViewController()
    if (target != null && top != null) {
        val sheet =
            UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(target.toString())),
                applicationActivities = null,
            )
        // A tablet shows the sheet as a popover, which needs a place to come from.
        sheet.popoverPresentationController?.sourceView = top.view
        sheet.popoverPresentationController?.sourceRect = top.view.bounds
        top.presentViewController(sheet, animated = true, completion = null)
    }
}
