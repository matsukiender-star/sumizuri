package com.sumizuri.app.ui.kit.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.art.InkLinearProgressIndicator
import com.sumizuri.app.ui.kit.input.BackButton
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.motionMs

// The way back for a page opened over another, given by the page around it, so its bar can show the arrow.
val LocalPageBack = compositionLocalOf<(() -> Unit)?> { null }

// True where a page shows no back arrow although it has a way back, such as the first page beside the list of the settings on a wide window, where the list is the way to another page.
val LocalHideBack = compositionLocalOf { false }

// The bar on top of every page: the name, a short note under it, the actions on the right, and a thin line along the bottom while something runs.
@Composable
fun PageTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    // How far a running job has got, from 0 to 1, or null when nothing runs.
    progress: Float? = null,
    // A back arrow before the name, for a page opened over another.
    onBack: (() -> Unit)? = null,
    // A small picture before the name, such as the icon of a source.
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val onBack = if (LocalHideBack.current) null else onBack ?: LocalPageBack.current
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(
                start = if (onBack !=
                    null
                ) {
                    4.dp
                } else {
                    PageGutter
                },
                end = 4.dp,
                top = 8.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BackButton(onBack, Modifier.padding(end = 4.dp))
            if (leading != null) Box(Modifier.padding(end = 12.dp)) { leading() }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // The line under the name keeps its room when it is empty, so the name sits in the same place on every page.
                Text(
                    subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actions()
        }
        val ms = motionMs(AppMotion.Fast)
        AnimatedVisibility(progress != null, enter = fadeIn(tween(ms)), exit = fadeOut(tween(ms))) {
            InkLinearProgressIndicator(
                progress = { progress ?: 1f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = PageGutter).height(3.dp),
            )
        }
    }
}
