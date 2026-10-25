package com.sumizuri.app.ui.text

import androidx.compose.runtime.Composable
import com.sumizuri.app.core.AppBuild
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.app_name
import com.sumizuri.app.shared.resources.untitled
import org.jetbrains.compose.resources.stringResource

// The name of a title, or a word for it when a source gave none.
@Composable
fun Title.displayName(): String = name.ifBlank { stringResource(Res.string.untitled) }

// The name of the app, for places outside the shared screens such as the title of the desktop window. A debug build puts Debug in front.
@Composable
fun appName(): String = stringResource(Res.string.app_name).let { if (AppBuild.isDebug) "Debug $it" else it }
