package com.sumizuri.app.feature.title.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.back
import com.sumizuri.app.ui.kit.page.PageGutter
import org.jetbrains.compose.resources.stringResource

// How tall the bar is without the room the system takes at the top.
private val BarHeight = 56.dp

// The room the bar takes over the top of the page, with the room the status bar takes above it. A title page runs under the bars of the system, so its cover shows behind the status bar, and only its own pieces keep clear of them.
@Composable
internal fun titleBarRoom(): Dp = BarHeight + WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

// The room around the list of a title page: the gutters at the sides, the bar at the top and a margin at the bottom, each with the room the system takes at that edge.
@Composable
internal fun titleListPadding(): PaddingValues {
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = PageGutter + safe.calculateStartPadding(direction),
        end = PageGutter + safe.calculateEndPadding(direction),
        top = titleBarRoom(),
        bottom = 24.dp + safe.calculateBottomPadding(),
    )
}

// The back arrow over the top of the page. Over the cover it sits on a round shade, and as the page scrolls up the bar turns into a plain strip and the name of the title shows in it. Progress from 0 to 1 tells how far.
@Composable
internal fun TitleBar(
    title: String,
    progress: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    soft: Boolean = false,
) {
    val page = MaterialTheme.colorScheme.background
    // A soft bar keeps the shade of its button, since no strip comes in behind it.
    val shade = IconButtonDefaults.iconButtonColors(
        containerColor = page.copy(
            alpha =
            0.55f * (if (soft) 1f else 1f - progress),
        ),
    )
    Row(
        modifier
            .fillMaxWidth()
            .then(if (soft) Modifier.softStrip(page, progress) else Modifier.background(page.copy(alpha = progress)))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onBack,
            colors = shade,
        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back)) }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 8.dp).alpha(progress),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// How far below the bar a soft strip fades out.
private val SoftReach = 28.dp

// A strip behind the bar that fades out at both ends and below it, so it lies over art without a hard edge.
private fun Modifier.softStrip(page: Color, progress: Float): Modifier = drawBehind {
    if (progress <= 0f) return@drawBehind
    val reach = SoftReach.toPx()
    val tall = size.height + reach
    val shown = page.copy(alpha = 0.92f * progress)
    val clear = page.copy(alpha = 0f)
    // Rows down the strip, each faded toward both ends, and fainter the further below the bar.
    val rows = 8
    for (row in 0 until rows) {
        val top = tall * row / rows
        val below = ((top - size.height) / reach).coerceIn(0f, 1f)
        val strength = 1f - below * below * (3f - 2f * below)
        drawRect(
            Brush.horizontalGradient(
                0f to clear,
                0.08f to shown.copy(alpha = shown.alpha * strength),
                0.75f to shown.copy(alpha = shown.alpha * strength),
                1f to clear,
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, tall / rows + 1f),
        )
    }
}
