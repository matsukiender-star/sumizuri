package com.sumizuri.app.feature.library.part

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_hover_continue
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.stringResource

private val PageCardWidth = 300.dp
private val PageCardHeight = 200.dp

// A large card for a title being read: the page the person stopped on, or the cover until a page was kept, fading into its name, the chapter and a button that goes on from there.
@Composable
internal fun PageCard(item: FeedItem, scale: Float, onOpen: (Long) -> Unit, onContinue: (Long) -> Unit) {
    val snapshot = remember(item.title.id, item.at) {
        com.sumizuri.app.ui.kit.image.PageSnapshots
            .of(item.title.id)
    }
    AppCard(Modifier.width(PageCardWidth).height(PageCardHeight), onClick = { onOpen(item.title.id) }) {
        Box(Modifier.fillMaxSize()) {
            if (snapshot != null) {
                AsyncImage(
                    snapshot,
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                )
            } else {
                CoverArt(item.title, Modifier.fillMaxSize(), letterSize = 40)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)),
                ),
            )
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (snapshot !=
                    null
                ) {
                    CoverArt(item.title, Modifier.width(40.dp).clip(MaterialTheme.shapes.small), letterSize = 12)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        numberedUnit(item.title.kind, item.number),
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                InkButton({ onContinue(item.title.id) }) {
                    Icon(
                        if (item.title.kind ==
                            Kind.Anime
                        ) {
                            Icons.Outlined.PlayArrow
                        } else {
                            Icons.AutoMirrored.Outlined.MenuBook
                        },
                        null,
                        Modifier.size(18.dp),
                    )
                    Text(stringResource(Res.string.library_hover_continue), Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
