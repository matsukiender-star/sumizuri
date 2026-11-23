package com.sumizuri.app.feature.sync.pair

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.account.PairLink
import com.sumizuri.app.data.net.rememberLocalNetworkAccess
import com.sumizuri.app.data.profile.LocalProfiles
import com.sumizuri.app.feature.sync.Working
import com.sumizuri.app.navigation.AppLinks
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.pair_bad_link
import com.sumizuri.app.shared.resources.pair_join
import com.sumizuri.app.shared.resources.pair_join_text
import com.sumizuri.app.shared.resources.pair_join_title
import com.sumizuri.app.shared.resources.pair_joined_already
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import org.jetbrains.compose.resources.stringResource

// A scanned code never links by itself: anyone can make a link, and a library would go to whoever made it.
@Composable
internal fun PairJoinDialog(model: PairModel) {
    val text by AppLinks.homePair.collectAsStateWithLifecycle()
    val shown = text ?: return
    val account by LocalProfiles.current.syncAccounts.account
        .collectAsStateWithLifecycle()
    val ui by model.ui.collectAsStateWithLifecycle()
    val askHomeNetwork = rememberLocalNetworkAccess()
    val link = remember(shown) { PairLink.parse(shown) }
    val busy = ui.joining
    val problem = ui.joinProblem
    val canJoin = link != null && link.fits(account)
    AppDialog(
        onDismissRequest = { if (!busy) model.dismissJoin() },
        title = { Text(stringResource(Res.string.pair_join_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        link == null -> stringResource(Res.string.pair_bad_link)
                        !canJoin && problem == null && !busy -> stringResource(Res.string.pair_joined_already)
                        else -> stringResource(Res.string.pair_join_text, link.addresses.first().substringBefore(':'))
                    },
                )
                if (busy) Working()
                problem?.let { PairProblem(it) }
            }
        },
        confirmButton = {
            if (canJoin) {
                InkButton({ askHomeNetwork { model.join(link) } }, enabled = !busy) {
                    Text(stringResource(Res.string.pair_join))
                }
            }
        },
        dismissButton = { CancelButton(model::dismissJoin, enabled = !busy) },
    )
}
