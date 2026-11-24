package com.sumizuri.app.ui.kit.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.challenge_open
import com.sumizuri.app.shared.resources.suki_err_challenge_hint
import com.sumizuri.app.shared.resources.suki_err_challenge_title
import com.sumizuri.app.shared.resources.suki_err_copied
import com.sumizuri.app.shared.resources.suki_err_copy_details
import com.sumizuri.app.shared.resources.suki_err_generic_hint
import com.sumizuri.app.shared.resources.suki_err_generic_title
import com.sumizuri.app.shared.resources.suki_err_hide_details
import com.sumizuri.app.shared.resources.suki_err_http_hint
import com.sumizuri.app.shared.resources.suki_err_http_title
import com.sumizuri.app.shared.resources.suki_err_network_hint
import com.sumizuri.app.shared.resources.suki_err_network_title
import com.sumizuri.app.shared.resources.suki_err_show_details
import com.sumizuri.app.shared.resources.suki_err_timeout_hint
import com.sumizuri.app.shared.resources.suki_err_timeout_title
import com.sumizuri.app.shared.resources.try_again
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.input.ActionButton
import com.sumizuri.app.ui.kit.input.rememberCopyText
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Maps raw technical errors into a human-friendly icon, title and explanation.
internal data class FriendlyErrorInfo(
    val icon: ImageVector,
    val title: StringResource,
    val hint: StringResource,
    val isChallenge: Boolean = false,
)

internal fun classifyError(raw: String, hasChallenge: Boolean): FriendlyErrorInfo {
    val lower = raw.lowercase()
    return when {
        hasChallenge || "cloudflare" in lower || "challenge" in lower || "captcha" in lower || "bot check" in lower -> {
            FriendlyErrorInfo(
                Icons.Outlined.Shield,
                Res.string.suki_err_challenge_title,
                Res.string.suki_err_challenge_hint,
                isChallenge = true,
            )
        }

        "timeout" in lower || "timed out" in lower -> {
            FriendlyErrorInfo(
                Icons.Outlined.HourglassEmpty,
                Res.string.suki_err_timeout_title,
                Res.string.suki_err_timeout_hint,
            )
        }

        "unknownhost" in lower || "unresolved" in lower || "no address" in lower || "connect" in lower ||
            "network" in lower ||
            "offline" in lower ||
            "unreachable" in lower -> {
            FriendlyErrorInfo(
                Icons.Outlined.CloudOff,
                Res.string.suki_err_network_title,
                Res.string.suki_err_network_hint,
            )
        }

        "404" in lower || "500" in lower || "502" in lower || "503" in lower || "504" in lower || "http " in lower -> {
            FriendlyErrorInfo(Icons.Outlined.Language, Res.string.suki_err_http_title, Res.string.suki_err_http_hint)
        }

        else -> FriendlyErrorInfo(
            Icons.Outlined.ErrorOutline,
            Res.string.suki_err_generic_title,
            Res.string.suki_err_generic_hint,
        )
    }
}

// Shared friendly error card with hidden-by-default copyable technical details and optional Cloudflare challenge action.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendlyErrorCard(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSolve: (() -> Unit)? = null,
) {
    val info = remember(error, onSolve != null) { classifyError(error, onSolve != null) }
    val colors = MaterialTheme.colorScheme
    var detailsOpen by remember(error) { mutableStateOf(false) }
    var copied by remember(error) { mutableStateOf(false) }
    val copy = rememberCopyText()

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800)
            copied = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(
                            if (info.isChallenge) colors.primaryContainer else colors.errorContainer,
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        info.icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (info.isChallenge) colors.onPrimaryContainer else colors.onErrorContainer,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(info.title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(info.hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onSolve != null) {
                    InkButton(onClick = onSolve) { Text(stringResource(Res.string.challenge_open)) }
                }
                ActionButton(Res.string.try_again, onRetry)
                TextButton(onClick = { detailsOpen = !detailsOpen }) {
                    Text(
                        stringResource(
                            if (detailsOpen) Res.string.suki_err_hide_details else Res.string.suki_err_show_details,
                        ),
                    )
                }
            }

            AnimatedVisibility(visible = detailsOpen) {
                TechnicalDetailsBox(
                    error = error,
                    copied = copied,
                    onCopy = {
                        copy(error)
                        copied = true
                    },
                )
            }
        }
    }
}

// Compact single-source error strip for Global Search so retrying never clears other sources.
@Composable
fun CompactErrorRow(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier, onSolve: (() -> Unit)? = null) {
    val info = remember(error, onSolve != null) { classifyError(error, onSolve != null) }
    val colors = MaterialTheme.colorScheme
    var detailsOpen by remember(error) { mutableStateOf(false) }
    var copied by remember(error) { mutableStateOf(false) }
    val copy = rememberCopyText()

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800)
            copied = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    info.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (info.isChallenge) colors.primary else colors.error,
                )
                Text(
                    stringResource(info.title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onSolve != null) {
                    TextButton(onClick = onSolve) {
                        Text(stringResource(Res.string.challenge_open), style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(onClick = { detailsOpen = !detailsOpen }) {
                    Text(
                        stringResource(
                            if (detailsOpen) Res.string.suki_err_hide_details else Res.string.suki_err_show_details,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Refresh, stringResource(Res.string.try_again), Modifier.size(18.dp))
                }
            }
            AnimatedVisibility(visible = detailsOpen) {
                TechnicalDetailsBox(
                    error = error,
                    copied = copied,
                    onCopy = {
                        copy(error)
                        copied = true
                    },
                )
            }
        }
    }
}

@Composable
private fun TechnicalDetailsBox(error: String, copied: Boolean, onCopy: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SelectionContainer {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(15.dp))
                    Text(
                        stringResource(if (copied) Res.string.suki_err_copied else Res.string.suki_err_copy_details),
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
