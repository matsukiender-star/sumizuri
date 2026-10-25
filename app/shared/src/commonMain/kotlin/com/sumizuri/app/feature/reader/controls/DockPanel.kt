package com.sumizuri.app.feature.reader.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.close
import org.jetbrains.compose.resources.stringResource

// What the dock holds open above its track: nothing, the light, the reading modes, the settings, the chapter list, the actions of a page, or the meaning of a word. The dock grows to hold it, so the reader has no sheets.
enum class DockPanel { None, Light, Mode, Settings, Chapters, Page, Lookup }
