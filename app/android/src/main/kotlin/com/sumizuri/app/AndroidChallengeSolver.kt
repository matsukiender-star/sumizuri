package com.sumizuri.app

import android.content.Context
import android.content.Intent
import com.sumizuri.app.source.browser.ChallengeSolver
import kotlinx.coroutines.CompletableDeferred

// Opens the screen with the site in it and waits until the person closes it.
class AndroidChallengeSolver(private val context: Context) : ChallengeSolver {
    override suspend fun solve(url: String) {
        val done = CompletableDeferred<Unit>()
        ChallengeActivity.waiting = done
        context.startActivity(
            Intent(
                context,
                ChallengeActivity::class.java,
            ).putExtra(ChallengeActivity.EXTRA_URL, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        done.await()
    }
}
