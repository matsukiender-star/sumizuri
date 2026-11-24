package com.sumizuri.app.feature.sync.pair

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.account.SyncProblem
import com.sumizuri.app.data.homeserver.HomePairing
import com.sumizuri.app.feature.sync.Problem
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.pair_error_no_network
import com.sumizuri.app.shared.resources.pair_error_not_started
import com.sumizuri.app.shared.resources.pair_error_other_server
import com.sumizuri.app.shared.resources.pair_error_over
import com.sumizuri.app.shared.resources.pair_error_unreachable
import com.sumizuri.sync.core.ErrorCodes
import org.jetbrains.compose.resources.stringResource

// What went wrong while pairing, with words of its own where the words of sync would mislead.
@Composable
internal fun PairProblem(problem: SyncProblem) {
    val words = when (problem.code) {
        ErrorCodes.PairOver -> Res.string.pair_error_over
        HomePairing.OtherServer -> Res.string.pair_error_other_server
        HomePairing.NotStarted -> Res.string.pair_error_not_started
        HomePairing.NoHomeNetwork -> Res.string.pair_error_no_network
        SyncProblem.Unreachable -> Res.string.pair_error_unreachable
        else -> null
    }
    Problem(if (words == null) problem else SyncProblem(PairWords, stringResource(words)))
}

private const val PairWords = "pair_words"
