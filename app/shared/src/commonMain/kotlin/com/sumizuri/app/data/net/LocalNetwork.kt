package com.sumizuri.app.data.net

import androidx.compose.runtime.Composable

// Gives a call that asks to reach the home network where the system wants that, then goes on either way, so a no ends in the usual error.
@Composable
expect fun rememberLocalNetworkAccess(): (then: () -> Unit) -> Unit
