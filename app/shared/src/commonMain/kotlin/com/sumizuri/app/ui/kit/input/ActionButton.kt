package com.sumizuri.app.ui.kit.input

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.back
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// An outlined button with a word on it, used for retry, next and the like.
@Composable
fun ActionButton(text: StringResource, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    InkOutlinedButton(onClick, modifier, enabled = enabled) { Text(stringResource(text)) }
}

// The arrow that closes a page, the same on every page.
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onBack, modifier) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(Res.string.back)) }
}
