package com.sumizuri.app.ui.kit.chips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// A heading and the choices under it as chips, like the choice section, for choices whose words are worked out as they are drawn, such as a number with a unit.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceWords(
    title: StringResource,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionLabel(stringResource(title))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (option in options) FilterChip(option == selected, { onSelect(option) }, { Text(label(option)) })
        }
    }
}
