package com.sumizuri.app.ui.kit.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// The corners of a dialog, round like the docks and panels of the reader.
val DialogShape = RoundedCornerShape(28.dp)

// The least and most width of a dialog, so it is never a thin strip nor stretched across a wide window.
private val LeastWidth = 300.dp
private val MostWidth = 560.dp

// Every dialog of the app in the way of the panels of the reader: a rounded panel with a thin edge, the title on top, what it says or asks under it, and its buttons at the end of the bottom row. It takes the same parts in the same order as the dialog of Material, so a dialog moves to it by its name alone. The shape the caller gives is not used, so every dialog has the same corners.
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") shape: Shape? = null,
    properties: DialogProperties = DialogProperties(),
) {
    val colors = MaterialTheme.colorScheme
    Dialog(
        onDismissRequest,
        DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            Modifier.padding(24.dp).widthIn(min = LeastWidth, max = MostWidth).then(modifier),
            shape = DialogShape,
            color = colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(Modifier.padding(top = 20.dp, bottom = 16.dp)) {
                if (title != null || icon != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (icon != null) CompositionLocalProvider(LocalContentColor provides colors.primary) { icon() }
                        if (title != null) {
                            Box(Modifier.weight(1f)) {
                                ProvideTextStyle(
                                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                ) { title() }
                            }
                        }
                    }
                }
                if (text != null) {
                    Box(Modifier.weight(1f, fill = false).padding(horizontal = 22.dp)) {
                        CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                        }
                    }
                }
                if (confirmButton != null || dismissButton != null) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dismissButton?.invoke()
                        confirmButton?.invoke()
                    }
                }
            }
        }
    }
}
