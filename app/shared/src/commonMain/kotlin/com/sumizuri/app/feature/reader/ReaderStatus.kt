package com.sumizuri.app.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.challenge_open
import com.sumizuri.app.shared.resources.try_again
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.input.ActionButton
import com.sumizuri.app.ui.kit.input.BackButton
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.stringResource

// What the reader shows while the chapter is coming in, or why it cannot be shown: one card in the middle with the chapter and its title, a spinner while it loads, and when it failed the reason with the ways out under it.
@Composable
fun ReaderStatus(state: ReaderState, onRetry: () -> Unit, onSolve: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
        }
        Box(Modifier.fillMaxSize().padding(PageGutter), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        state.chapterName ?: numberedUnit(state.kind, state.chapterNumber),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.titleName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val message = state.message
                    Box(Modifier.padding(top = 18.dp), contentAlignment = Alignment.Center) {
                        if (message == null) {
                            InkCircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.ErrorOutline,
                                    null,
                                    Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    messageText(message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                                InkButton(onRetry) { Text(stringResource(Res.string.try_again)) }
                                if (state.challengeUrl != null) ActionButton(Res.string.challenge_open, onSolve)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun messageText(message: ReaderMessage): String = when (message) {
    is ReaderMessage.Fixed -> stringResource(message.text)
    is ReaderMessage.Detail -> message.text
}
