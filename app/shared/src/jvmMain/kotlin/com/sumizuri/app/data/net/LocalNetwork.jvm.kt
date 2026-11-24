package com.sumizuri.app.data.net

import androidx.compose.runtime.Composable

// A computer reaches its home network without asking.
@Composable
actual fun rememberLocalNetworkAccess(): (then: () -> Unit) -> Unit = { it() }
