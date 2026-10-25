package com.sumizuri.app.feature.settings.look

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.motion.PageMotion
import com.sumizuri.app.ui.motion.pageTransform
import com.sumizuri.app.ui.motion.rememberMotionShift
import kotlinx.coroutines.delay

// How long the preview rests on each page before it moves again.
private const val RestMs = 1_100L

// A small screen that opens a page and goes back again over and over, in the motion that is chosen, so it can be seen before it is used.
@Composable
internal fun MotionPreview(style: PageMotion) {
    val ms = style.pageMs()
    val shift = rememberMotionShift()
    var depth by remember { mutableIntStateOf(0) }
    LaunchedEffect(style) {
        while (true) {
            delay(RestMs + ms)
            depth = if (depth == 0) 1 else 0
        }
    }
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 132.dp, height = 200.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceContainerLowest)
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp)),
        ) {
            AnimatedContent(depth, transitionSpec = {
                pageTransform(style, ms, targetState > initialState, shift) using
                    null
            }, label = "preview") { shown ->
                MockPage(
                    if (shown ==
                        0
                    ) {
                        colors.surfaceContainer
                    } else {
                        colors.surfaceContainerHigh
                    },
                    if (shown ==
                        0
                    ) {
                        colors.secondary
                    } else {
                        colors.primary
                    },
                    cards = shown == 0,
                )
            }
        }
    }
}

// A page drawn in blocks: a bar at the top, then a grid of covers for the first page and lines of text for the one it opens.
@Composable
private fun MockPage(fill: Color, accent: Color, cards: Boolean) {
    Column(Modifier.fillMaxSize().background(fill).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .width(56.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent),
        )
        if (cards) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(
                                    width = 32.dp,
                                    height = 44.dp,
                                ).clip(RoundedCornerShape(6.dp))
                                .background(accent.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.45f)),
            )
            repeat(5) { line ->
                Box(
                    Modifier
                        .fillMaxWidth(if (line % 2 == 0) 0.9f else 0.65f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.3f)),
                )
            }
        }
    }
}
