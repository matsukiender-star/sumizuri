package com.sumizuri.app.feature.library.tile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_hover_continue
import com.sumizuri.app.shared.resources.library_hover_done
import com.sumizuri.app.shared.resources.library_hover_read
import com.sumizuri.app.ui.theme.LocalThemeSpec
import org.jetbrains.compose.resources.stringResource

// A cover that, while the pointer of a computer rests on it, dims a little and shows a round button in the middle that goes on reading or watching without opening the page of the title, with how far it is under it. A touch screen never rests a pointer, so phones show the cover alone.
@Composable
internal fun HoverCover(title: Title, hovered: Boolean, onContinue: (() -> Unit)?, content: @Composable () -> Unit) {
    val shape = LocalThemeSpec.current.shapes.cover
        .toShape()
    Box {
        content()
        AnimatedVisibility(
            hovered && onContinue != null,
            Modifier.matchParentSize().clip(shape),
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(120)),
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.animateEnterExit(
                        enter = scaleIn(
                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            initialScale = 0.7f,
                        ),
                        exit = scaleOut(tween(120), targetScale = 0.85f),
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ContinueDot(title.kind, stringResource(Res.string.library_hover_continue)) { onContinue?.invoke() }
                    val read = title.chapterCount - title.unread
                    val said = when {
                        title.chapterCount <= 0 -> null
                        title.unread <= 0 -> stringResource(Res.string.library_hover_done)
                        else -> stringResource(Res.string.library_hover_read, read, title.chapterCount)
                    }
                    if (said != null) {
                        Text(
                            said,
                            Modifier
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// The round button, a play mark for anime and a book for the rest, that grows a little under the pointer.
@Composable
private fun ContinueDot(kind: Kind, label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val over by source.collectIsHoveredAsState()
    Box(
        Modifier
            .scale(if (over) 1.08f else 1f)
            .size(48.dp)
            .shadow(8.dp, CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(source, null, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (kind == Kind.Anime) Icons.Rounded.PlayArrow else Icons.AutoMirrored.Rounded.MenuBook,
            label,
            Modifier.size(if (kind == Kind.Anime) 28.dp else 22.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
