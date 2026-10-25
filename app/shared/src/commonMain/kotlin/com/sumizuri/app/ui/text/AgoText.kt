package com.sumizuri.app.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sumizuri.app.data.text.AgoUnit
import com.sumizuri.app.data.text.agoSince
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ago_days
import com.sumizuri.app.shared.resources.ago_hours
import com.sumizuri.app.shared.resources.ago_minutes
import com.sumizuri.app.shared.resources.ago_now
import com.sumizuri.app.shared.resources.ago_yesterday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// A time as words such as 5 days ago, read against the clock when the screen is drawn.
@Composable
fun agoText(at: Long): String {
    val now = remember { nowMillis() }
    val gap = agoSince(now, at)
    return when (gap.unit) {
        AgoUnit.Now -> stringResource(Res.string.ago_now)
        AgoUnit.Yesterday -> stringResource(Res.string.ago_yesterday)
        AgoUnit.Minutes -> pluralStringResource(Res.plurals.ago_minutes, gap.count, gap.count)
        AgoUnit.Hours -> pluralStringResource(Res.plurals.ago_hours, gap.count, gap.count)
        AgoUnit.Days -> pluralStringResource(Res.plurals.ago_days, gap.count, gap.count)
    }
}
