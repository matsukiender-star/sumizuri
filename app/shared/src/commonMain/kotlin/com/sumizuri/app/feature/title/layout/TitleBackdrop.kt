package com.sumizuri.app.feature.title.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.feature.title.rememberTitleFocus
import com.sumizuri.app.ui.kit.cover.coverDrift
import com.sumizuri.app.ui.kit.cover.fadeOutBottom
import com.sumizuri.app.ui.kit.cover.focus.focusAlignment
import com.sumizuri.app.ui.kit.cover.focus.rememberSoftCover
import com.sumizuri.app.ui.kit.image.imageRequest

// The cover across the top of the wide page, soft and darkened behind the text, fading to clear where the chapters begin. It scrolls up with the page, so it is behind the banner only. The cover is read small and blurred once, so it costs nothing while the page is drawn, and it shows without fading in.
@Composable
internal fun TitleBackdrop(title: Title, height: Dp, picked: Pair<Float, Float>?, scrolled: () -> Int) {
    val url = title.coverUrl
    if (url.isNullOrBlank()) return
    val focus = rememberTitleFocus(url, picked)
    val soft = rememberSoftCover(imageRequest(url, sourceId = title.sourceId))
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { translationY = -scrolled().coerceAtLeast(0).toFloat() }
            .clipToBounds()
            .fadeOutBottom(0.6f),
    ) {
        if (soft != null) {
            Image(
                soft,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().coverDrift(),
                // The crop is centered on the face found in the cover.
                alignment = focusAlignment(focus, maxWidth.value, height.value),
                contentScale = ContentScale.Crop,
                // The smoothest stretch, so the blurred pixels flow into each other.
                filterQuality = FilterQuality.High,
            )
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)))
        // A little black on top of that, since a bright cover still washes the words out.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
    }
}
