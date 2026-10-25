package com.sumizuri.app.feature.title.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.feature.title.chapter.ContinueTarget
import com.sumizuri.app.feature.title.chapter.spotText
import com.sumizuri.app.feature.title.layout.TileHeight
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.title_continue_next
import com.sumizuri.app.shared.resources.title_continue_resume
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkTonalButton
import org.jetbrains.compose.resources.stringResource

// The big button that opens the chapter after the furthest read. When another chapter was begun or read lately, a second button of the same size picks that one up again, under it or, on the wide page, beside it.
@Composable
internal fun ContinueButton(
    target: ContinueTarget?,
    kind: Kind,
    onContinue: (Long) -> Unit,
    modifier: Modifier = Modifier,
    side: Boolean = false,
) {
    if (target == null) return
    val gap = Arrangement.spacedBy(8.dp)
    val fit = if (side) Modifier.height(TileHeight) else Modifier.fillMaxWidth()
    val body: @Composable (Modifier) -> Unit = { each ->
        InkButton({ onContinue(target.id) }, each.then(fit)) {
            Icon(Icons.Outlined.PlayArrow, null)
            // The chapter is named as the list names it, which for a site that numbers in two ways is not its number.
            Text(
                stringResource(
                    Res.string.title_continue_next,
                    target.name ?: spotText(kind, target.number, target.season),
                ),
                Modifier.padding(start = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val otherId = target.otherId
        val otherNumber = target.otherNumber
        if (otherId != null && otherNumber != null) {
            InkTonalButton({ onContinue(otherId) }, each.then(fit)) {
                Icon(Icons.Outlined.History, null)
                Text(
                    stringResource(
                        Res.string.title_continue_resume,
                        target.otherName ?: spotText(kind, otherNumber, target.otherSeason),
                    ),
                    Modifier.padding(start = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    if (side) {
        Row(modifier, horizontalArrangement = gap) {
            body(Modifier.weight(1f))
        }
    } else {
        Column(modifier, verticalArrangement = gap) { body(Modifier) }
    }
}
