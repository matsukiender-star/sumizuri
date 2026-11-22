package com.sumizuri.app.ui.kit.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator

// What a page shows while it waits for a source, a small spinner, and when it failed a friendly card with retry and optional challenge solver.
@Composable
fun LoadStatus(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSolve: (() -> Unit)? = null,
) {
    if (!loading && error == null) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                InkCircularProgressIndicator(strokeWidth = 2.dp)
            }
        }
        if (error != null) {
            FriendlyErrorCard(error = error, onRetry = onRetry, onSolve = onSolve)
        }
    }
}
