package com.sumizuri.app.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.data.profile.Profile
import com.sumizuri.app.data.text.monthYearText
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.profile_edit_short
import com.sumizuri.app.shared.resources.profile_reading_since
import com.sumizuri.app.shared.resources.profile_switch
import org.jetbrains.compose.resources.stringResource

// The top of the profile: the picture in a ring, the name set large, since when the reader has read, and the ways to switch or edit.
@Composable
internal fun ProfileHeader(profile: Profile, onSwitch: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ProfileAvatar(
            profile.name,
            profile.avatar,
            80.dp,
            Modifier
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .clickable(onClick = onEdit),
        )
        Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
            Text(
                profile.name,
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.profile_reading_since, monthYearText(profile.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onSwitch) { Text(stringResource(Res.string.profile_switch)) }
                TextButton(onEdit) { Text(stringResource(Res.string.profile_edit_short)) }
            }
        }
    }
}
