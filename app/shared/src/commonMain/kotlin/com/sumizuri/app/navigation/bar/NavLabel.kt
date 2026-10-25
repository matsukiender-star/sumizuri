package com.sumizuri.app.navigation.bar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.art.StretchDot

// The name of a tab with a small bar under it that stretches out when the tab is chosen and shrinks away when another is.
@Composable
internal fun NavLabel(text: String, selected: Boolean, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
        StretchDot(selected, Modifier.padding(top = 3.dp), idle = 0.dp, wide = 18.dp, height = 4.dp)
    }
}
