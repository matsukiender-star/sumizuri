package com.sumizuri.app.feature.player.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.sumizuri.app.playback.VideoThumbnail

// How wide the picture over the seek bar is drawn.
private val PreviewWidth = 176.dp

// A small picture of the video at the spot the pointer or the finger is on, with its time under it, floating just over the seek bar. The picture of the last spot stays until the new one comes, so it never blinks. The middle is in pixels from the left of the bar.
@Composable
internal fun SeekPreview(middle: Int, time: String, thumbnail: VideoThumbnail?) {
    Popup(popupPositionProvider = Above(middle)) {
        Column(
            Modifier
                .width(PreviewWidth)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), MaterialTheme.shapes.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            thumbnail?.let {
                Image(
                    it.image,
                    null,
                    Modifier.width(PreviewWidth),
                    contentScale = ContentScale.FillWidth,
                )
            }
            Box(Modifier.padding(vertical = 4.dp)) {
                Text(time, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Puts the preview over its spot on the bar, a little above it, and keeps it inside the window.
private class Above(private val middle: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + middle - popupContentSize.width / 2).coerceIn(
            0,
            (
                windowSize.width -
                    popupContentSize.width
                ).coerceAtLeast(0),
        )
        val y = (anchorBounds.top - popupContentSize.height - 8).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
