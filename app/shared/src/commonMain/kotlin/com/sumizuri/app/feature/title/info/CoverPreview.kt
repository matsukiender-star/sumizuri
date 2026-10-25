package com.sumizuri.app.feature.title.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.ui.kit.dialog.wholeScreenDialog
import com.sumizuri.app.ui.kit.image.imageRequest
import com.sumizuri.app.ui.kit.input.pinchZoom

// The cover across the whole window, which can be pinched and dragged. A tap closes it.
@Composable
internal fun CoverPreview(title: Title, onDismiss: () -> Unit) {
    val url = title.coverUrl ?: return
    Dialog(onDismiss, wholeScreenDialog()) {
        Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).clickable(onClick = onDismiss)) {
            AsyncImage(
                imageRequest(url, sourceId = title.sourceId),
                title.name,
                Modifier.fillMaxSize().pinchZoom(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
