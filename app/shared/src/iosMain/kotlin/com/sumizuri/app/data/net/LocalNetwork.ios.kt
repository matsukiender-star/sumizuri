package com.sumizuri.app.data.net

import androidx.compose.runtime.Composable

// An iPhone asks by itself the first time the app reaches the home network.
@Composable
actual fun rememberLocalNetworkAccess(): (then: () -> Unit) -> Unit = { it() }
