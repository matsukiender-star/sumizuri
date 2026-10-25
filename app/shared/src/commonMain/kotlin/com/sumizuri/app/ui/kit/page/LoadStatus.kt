package com.sumizuri.app.ui.kit.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.challenge_open
import com.sumizuri.app.shared.resources.try_again
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.input.ActionButton

// What a page shows while it waits for a source, a small spinner, and when it failed the reason with a retry. When a bot check stopped it, there is also a way to open the site and pass the check.
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
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            if (onSolve != null) ActionButton(Res.string.challenge_open, onSolve)
            ActionButton(Res.string.try_again, onRetry)
        }
    }
}
