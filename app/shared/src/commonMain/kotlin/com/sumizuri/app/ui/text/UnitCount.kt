package com.sumizuri.app.ui.text

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.text.numberText
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.chapter_number
import com.sumizuri.app.shared.resources.chapters_count
import com.sumizuri.app.shared.resources.episode_number
import com.sumizuri.app.shared.resources.episodes_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

// 163 chapters or 12 episodes, with the plural of the language.
@Composable
fun unitCount(kind: Kind, count: Int): String = pluralStringResource(
    if (kind ==
        Kind.Anime
    ) {
        Res.plurals.episodes_count
    } else {
        Res.plurals.chapters_count
    },
    count,
    count,
)

// Chapter 12 or Episode 12, in the language of the screen.
@Composable
fun numberedUnit(kind: Kind, number: Double): String = stringResource(
    if (kind ==
        Kind.Anime
    ) {
        Res.string.episode_number
    } else {
        Res.string.chapter_number
    },
    number.numberText(),
)

// A size in bytes as a short number with a unit, such as 12.3 MB.
fun byteSizeText(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val shown = if (unit == 0 ||
        value >= 100
    ) {
        value.roundToLong().toString()
    } else {
        ((value * 10).roundToLong() / 10.0).toString()
    }
    return "$shown ${units[unit]}"
}
