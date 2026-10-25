package com.sumizuri.app.ui.kit.input

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.theme.LocalThemeSpec

// The switch of the app: a Material switch that has a check in its thumb when it is on, if the theme asks for icons in switches.
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val icons = LocalThemeSpec.current.options.controls.switchIcons
    Switch(
        checked,
        onCheckedChange,
        modifier,
        enabled = enabled,
        thumbContent = if (icons &&
            checked
        ) {
            ({ Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) })
        } else {
            null
        },
    )
}
