package com.sumizuri.app.ui.kit.input

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import okio.Path
import java.io.File

// The folder of the cache the share sheet reads from, which the file provider of the app lists.
private const val SharedFolder = "shared"

actual fun canShareFiles(): Boolean = true

@Composable
actual fun rememberFileSharer(): (name: String, file: Path) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { name, file ->
            runCatching {
                val folder = File(context.cacheDir, SharedFolder).apply { mkdirs() }
                val copy = File(folder, name)
                File(file.toString()).copyTo(copy, overwrite = true)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", copy)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mimeOf(name)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}

private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "avif" -> "image/avif"
    else -> "image/jpeg"
}
