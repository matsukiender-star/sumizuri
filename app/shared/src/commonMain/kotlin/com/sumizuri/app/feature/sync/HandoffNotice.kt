package com.sumizuri.app.feature.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.handoff_continue
import com.sumizuri.app.shared.resources.handoff_title
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.card.AppCard
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// How long the offer stays before it goes by itself.
private const val ShownMs = 15_000L

// After a sync that brought reading from another device, an offer at the top to go on with that title here, from where it was left there.
@Composable
fun HandoffNotice(titleId: Long?, onContinue: (Long) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val library = LocalAppContainer.current.library
    val name by produceState<String?>(null, titleId) {
        value =
            titleId?.let { library.nameOf(it) }?.takeIf { it.isNotBlank() }
    }
    LaunchedEffect(titleId) {
        if (titleId == null) return@LaunchedEffect
        delay(ShownMs)
        onDismiss()
    }
    AnimatedVisibility(
        titleId != null && name != null,
        modifier,
        enter = slideInVertically { -it } + fadeIn(),
        exit =
        slideOutVertically { -it } + fadeOut(),
    ) {
        AppCard(Modifier.widthIn(max = 520.dp)) {
            Row(
                Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Devices, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.handoff_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        name.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                InkButton({ titleId?.let(onContinue) }) { Text(stringResource(Res.string.handoff_continue)) }
                IconButton(onDismiss) { Icon(Icons.Outlined.Close, null) }
            }
        }
    }
}
