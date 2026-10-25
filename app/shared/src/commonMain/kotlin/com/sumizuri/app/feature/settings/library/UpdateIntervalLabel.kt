package com.sumizuri.app.feature.settings.library

import androidx.compose.runtime.Composable
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.update_every_day
import com.sumizuri.app.shared.resources.update_every_days
import com.sumizuri.app.shared.resources.update_every_hour
import com.sumizuri.app.shared.resources.update_every_hours
import com.sumizuri.app.shared.resources.update_every_week
import com.sumizuri.app.shared.resources.update_interval_off
import org.jetbrains.compose.resources.stringResource

// The words for a number of hours between two checks of the library, with zero for no check on its own.
@Composable
internal fun updateIntervalLabel(hours: Int): String = when {
    hours <= 0 -> stringResource(Res.string.update_interval_off)
    hours == 1 -> stringResource(Res.string.update_every_hour)
    hours < 24 -> stringResource(Res.string.update_every_hours, hours)
    hours == 24 -> stringResource(Res.string.update_every_day)
    hours == 168 -> stringResource(Res.string.update_every_week)
    hours % 24 == 0 -> stringResource(Res.string.update_every_days, hours / 24)
    else -> stringResource(Res.string.update_every_hours, hours)
}
