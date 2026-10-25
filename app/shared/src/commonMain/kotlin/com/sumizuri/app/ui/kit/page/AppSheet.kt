package com.sumizuri.app.ui.kit.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sumizuri.app.ui.kit.dialog.DialogShape
import com.sumizuri.app.ui.kit.dock.PanelHead

// A window at least this wide shows a sheet as a panel in the middle, as a sheet across a wide window leaves its words far apart.
private val PanelFrom = 600.dp

// The most width of a sheet shown as a panel, and the most of the height of the window it takes.
private val PanelWidth = 560.dp
private const val PanelHeightShare = 0.86f

// A sheet of choices in the way of the panels of the reader, with its title and a cross on top and a column that scrolls when it is long. On a wide window it is a rounded panel in the middle, and on a phone it slides up from the bottom with the same corners and colors. It opens all the way at once, so nothing waits behind a drag up.
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    spacing: Dp = 12.dp,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val wide = with(density) { window.width.toDp() } >= PanelFrom
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(start = 10.dp, end = 6.dp, top = 6.dp)) {
            if (title !=
                null
            ) {
                PanelHead(title, onDismiss)
            }
        }
        Column(
            Modifier
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = PageGutter)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
    if (wide) {
        Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = PanelWidth)
                    .heightIn(max = with(density) { (window.height * PanelHeightShare).toDp() }),
                shape = DialogShape,
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
            ) { Column { body() } }
        }
        return
    }
    ModalBottomSheet(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colors.surfaceContainerHigh,
    ) { body() }
}
