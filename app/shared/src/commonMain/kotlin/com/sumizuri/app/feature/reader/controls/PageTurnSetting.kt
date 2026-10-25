package com.sumizuri.app.feature.reader.controls

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.reader.ReaderPageTurn
import com.sumizuri.app.data.model.reader.ReaderSettings
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.reader_page_turn
import com.sumizuri.app.shared.resources.reader_turn_off
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import org.jetbrains.compose.resources.stringResource

// One choice for how a page turns, with Off for no motion at all. It stands for two settings, whether pages move and how they move, which were shown as two that seemed to say the same.
@Composable
internal fun PageTurnSetting(settings: ReaderSettings, onSettings: (ReaderSettings) -> Unit) {
    val options: List<ReaderPageTurn?> = listOf(null) + ReaderPageTurn.entries
    ChoiceSetting(
        Res.string.reader_page_turn,
        options,
        settings.pageTurn.takeIf { settings.animatePages },
        { turn -> if (turn == null) stringResource(Res.string.reader_turn_off) else stringResource(turn.label) },
    ) { turn ->
        onSettings(
            if (turn ==
                null
            ) {
                settings.copy(animatePages = false)
            } else {
                settings.copy(animatePages = true, pageTurn = turn)
            },
        )
    }
}
