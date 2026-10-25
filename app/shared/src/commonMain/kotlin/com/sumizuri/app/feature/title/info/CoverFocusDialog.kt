package com.sumizuri.app.feature.title.info

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.focus_auto
import com.sumizuri.app.shared.resources.focus_hint
import com.sumizuri.app.shared.resources.focus_title
import com.sumizuri.app.shared.resources.save
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.cover.focus.rememberCoverFocus
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import com.sumizuri.app.ui.kit.image.imageRequest
import org.jetbrains.compose.resources.stringResource

// Lets a person tap where the backdrop should centre on the cover, for when the guess chose a hand or a logo over the face.
@Composable
internal fun CoverFocusDialog(
    title: Title,
    current: Pair<Float, Float>?,
    onSave: (Float, Float) -> Unit,
    onAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    val url = title.coverUrl ?: return
    val found by rememberCoverFocus(url)
    var picked by remember { mutableStateOf(current) }
    val point = picked ?: found?.let { it.x to it.y }
    fun at(spot: Offset, size: IntSize) =
        (spot.x / size.width).coerceIn(0f, 1f) to (spot.y / size.height).coerceIn(0f, 1f)
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.focus_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.focus_hint), style = MaterialTheme.typography.bodySmall)
                BoxWithConstraints(
                    Modifier
                        .width(220.dp)
                        .aspectRatio(found?.aspect ?: (2f / 3f))
                        .clip(MaterialTheme.shapes.small)
                        .pointerInput(Unit) { detectTapGestures { picked = at(it, size) } }
                        .pointerInput(Unit) { detectDragGestures { change, _ -> picked = at(change.position, size) } },
                ) {
                    AsyncImage(
                        imageRequest(url, sourceId = title.sourceId),
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    point?.let { (x, y) ->
                        Column(
                            Modifier
                                .offset(
                                    maxWidth * x - 22.dp,
                                    maxHeight * y - 22.dp,
                                ).size(44.dp)
                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        ) {
                        }
                    }
                }
                // Under the cover rather than beside the other buttons, which leaves only two there, so they fit side by side on a phone.
                TextButton(onAuto) { Text(stringResource(Res.string.focus_auto)) }
            }
        },
        confirmButton = {
            InkButton({
                picked?.let { (x, y) -> onSave(x, y) }
            }, enabled = picked != null) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { CancelButton(onDismiss) },
    )
}
