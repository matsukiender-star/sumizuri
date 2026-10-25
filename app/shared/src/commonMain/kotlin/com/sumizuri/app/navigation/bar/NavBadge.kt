package com.sumizuri.app.navigation.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.ui.theme.LocalReduceMotion

// The count of new chapters on a tab. It pops in with a spring each time the count changes, and past 99 it only says so.
@Composable
internal fun NavBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = MaterialTheme.colorScheme
    val pop = remember { Animatable(1f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(count) {
        if (reduce) return@LaunchedEffect
        pop.snapTo(0.55f)
        pop.animateTo(1f, spring(0.35f, Spring.StiffnessMedium))
    }
    Text(
        if (count > 99) "99+" else count.toString(),
        modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }.background(colors.primary, CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = colors.onPrimary,
        maxLines = 1,
    )
}
