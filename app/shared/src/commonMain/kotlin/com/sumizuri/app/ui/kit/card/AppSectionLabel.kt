package com.sumizuri.app.ui.kit.card

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// A small uppercase heading that groups rows.
@Composable
fun AppSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier.padding(top = 20.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.outline,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

// A small uppercase heading that groups rows, for a heading that is a resource.
@Composable
fun AppSectionLabel(text: StringResource, modifier: Modifier = Modifier) =
    AppSectionLabel(stringResource(text), modifier)
