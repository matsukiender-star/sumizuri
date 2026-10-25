package com.sumizuri.app.feature.title.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.feature.title.rememberTitleFocus
import com.sumizuri.app.ui.kit.cover.coverDrift
import com.sumizuri.app.ui.kit.cover.coverFlightTo
import com.sumizuri.app.ui.kit.cover.fadeOutBottom
import com.sumizuri.app.ui.kit.cover.focus.focusAlignment
import com.sumizuri.app.ui.kit.image.ImageFilter
import com.sumizuri.app.ui.kit.image.imageRequest

// How much slower than the list the cover moves up as the page scrolls.
private const val Lag = 0.4f

// The cover at the full width of the phone behind the top of the page. It moves up slower than the list, and its lower part fades to clear so the list melts into the page.
@Composable
internal fun TitleCoverBehind(title: Title, height: Dp, picked: Pair<Float, Float>?, scroll: () -> Int) {
    val stand = Brush.verticalGradient(
        listOf(Color.hsv(title.hue, 0.42f, 0.62f), Color.hsv((title.hue + 28f) % 360f, 0.55f, 0.30f)),
    )
    val focus = rememberTitleFocus(title.coverUrl, picked)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { translationY = -(scroll() * Lag).coerceAtMost(size.height) }
            .fadeOutBottom(0.45f)
            .background(stand),
    ) {
        val url = title.coverUrl
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = imageRequest(url, sourceId = title.sourceId),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .coverFlightTo(title.id)
                    .clipToBounds()
                    .coverDrift(),
                contentScale = ContentScale.Crop,
                // The crop is centered on the spot picked or the face found in the cover, and falls back to the top.
                alignment = focusAlignment(focus, maxWidth.value, height.value, fallbackY = -1f),
                filterQuality = ImageFilter,
            )
            // A dark layer where the name, the facts and the buttons go, so the small grey lines can be read over a bright cover too.
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        0.75f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
            )
        }
    }
}
