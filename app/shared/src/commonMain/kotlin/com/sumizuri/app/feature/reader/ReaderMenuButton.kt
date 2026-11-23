package com.sumizuri.app.feature.reader

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.reader_menu_button
import org.jetbrains.compose.resources.stringResource

// Suki: a see-through round button in the lower right corner that brings out the controls, so the menu of the reader is found without guessing where to tap.
@Composable
internal fun BoxScope.ReaderMenuButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .size(48.dp),
    ) {
        IconButton(onClick) {
            Icon(Icons.Outlined.Tune, stringResource(Res.string.reader_menu_button))
        }
    }
}
