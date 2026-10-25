package com.sumizuri.app.ui.kit.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sumizuri.app.data.local.LocalSources
import com.sumizuri.app.ui.kit.image.ImageFilter
import com.sumizuri.app.ui.kit.image.imageRequest

// A source's icon over a plain mark, which shows when the source has no icon or it does not load. The sources of files on the device show a folder.
@Composable
fun SourceMark(iconUrl: String, modifier: Modifier = Modifier, size: Dp = 40.dp, sourceId: String? = null) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (LocalSources.isLocal(sourceId)) Icons.Outlined.Folder else Icons.Outlined.Public,
            null,
            Modifier.size(size * 0.5f),
            tint = MaterialTheme.colorScheme.outline,
        )
        if (iconUrl.isNotBlank()) {
            AsyncImage(
                model = imageRequest(iconUrl),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                filterQuality = ImageFilter,
            )
        }
    }
}
