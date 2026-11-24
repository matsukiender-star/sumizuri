package com.sumizuri.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

internal const val PresetAvatarPrefix = "preset:"

// Pictures that come with the app, drawn from icons over a gradient, so a TV with no gallery can still give a profile a face. They work offline and add nothing to the size of the app. A profile keeps one as "preset:" and its name.
internal data class PresetAvatar(val id: String, val start: Color, val end: Color, val icon: ImageVector) {
    val key: String get() = "$PresetAvatarPrefix$id"
}

internal val PresetAvatars: List<PresetAvatar> = listOf(
    PresetAvatar("sakura", Color(0xFFEC4899), Color(0xFF9333EA), Icons.Outlined.LocalFlorist),
    PresetAvatar("moon", Color(0xFF4F46E5), Color(0xFF1E1B4B), Icons.Outlined.Bedtime),
    PresetAvatar("star", Color(0xFFF59E0B), Color(0xFFD97706), Icons.Outlined.AutoAwesome),
    PresetAvatar("bolt", Color(0xFF06B6D4), Color(0xFF2563EB), Icons.Outlined.Bolt),
    PresetAvatar("flame", Color(0xFFEF4444), Color(0xFFF97316), Icons.Outlined.LocalFireDepartment),
    PresetAvatar("wave", Color(0xFF0EA5E9), Color(0xFF0F766E), Icons.Outlined.Waves),
    PresetAvatar("forest", Color(0xFF10B981), Color(0xFF047857), Icons.Outlined.Park),
    PresetAvatar("pets", Color(0xFFA855F7), Color(0xFF6366F1), Icons.Outlined.Pets),
    PresetAvatar("movie", Color(0xFFE11D48), Color(0xFF9F1239), Icons.Outlined.Movie),
    PresetAvatar("music", Color(0xFFD946EF), Color(0xFF7C3AED), Icons.Outlined.MusicNote),
    PresetAvatar("game", Color(0xFF14B8A6), Color(0xFF0284C7), Icons.Outlined.SportsEsports),
    PresetAvatar("rocket", Color(0xFF3B82F6), Color(0xFF4338CA), Icons.Outlined.RocketLaunch),
)

internal fun presetAvatarOf(avatar: String?): PresetAvatar? {
    if (avatar == null || !avatar.startsWith(PresetAvatarPrefix)) return null
    val id = avatar.removePrefix(PresetAvatarPrefix)
    return PresetAvatars.firstOrNull { it.id == id }
}

@Composable
internal fun PresetAvatarBadge(preset: PresetAvatar, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(preset.start, preset.end))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = preset.icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.52f),
            tint = Color.White,
        )
    }
}
