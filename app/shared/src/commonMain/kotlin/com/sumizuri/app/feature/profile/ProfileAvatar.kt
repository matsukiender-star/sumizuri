package com.sumizuri.app.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import okio.Path.Companion.toPath

// A round picture of a reader. The first letter of the name stands in until the picture is there, and when there is none. A picture just chosen and not saved yet is given as a bitmap.
@Composable
fun ProfileAvatar(name: String, avatar: String?, size: Dp, modifier: Modifier = Modifier, chosen: ImageBitmap? = null) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        when {
            chosen != null -> Image(chosen, null, Modifier.size(size), contentScale = ContentScale.Crop)
            avatar != null -> AsyncImage(avatar.toPath(), null, Modifier.size(size), contentScale = ContentScale.Crop)
        }
    }
}
