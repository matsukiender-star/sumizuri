package com.sumizuri.app.source.browser

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.core.log.withoutQuery
import com.sumizuri.app.source.browser.page.RenderPage
import com.sumizuri.app.source.browser.page.decodePageAnswer
import com.sumizuri.app.source.browser.page.flag
import com.sumizuri.app.source.host.string
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal object ChallengeWaiter {
    private val PollInterval = 250.milliseconds
    private const val TAG = "challenge"

    // Waits while the page shows a bot check, up to the budget.
    suspend fun settle(page: RenderPage, budget: Duration = ChallengeSettleBudget) {
        val clock = TimeSource.Monotonic.markNow()
        var seen = false
        var reported = 0L
        while (true) {
            val state = read(page::eval)
            if (state.challenged && !seen) {
                seen = true
                AppLog.info(TAG, "a bot check is showing at ${state.url.withoutQuery()}, waiting for it to clear")
            }
            if (state.cleared) {
                if (seen) AppLog.info(TAG, "the bot check cleared after ${clock.elapsedNow().inWholeMilliseconds} ms")
                return
            }
            val seconds = clock.elapsedNow().inWholeSeconds
            if (seen && seconds >= reported + 5) {
                reported = seconds
                AppLog.debug(TAG, "still waiting on the bot check, $seconds seconds so far")
            }
            if (clock.elapsedNow() >= budget) {
                AppLog.warn(
                    TAG,
                    "gave up after ${budget.inWholeSeconds} seconds, bot check showing: ${state.challenged}, page loaded: ${state.complete}",
                )
                if (state.challenged) throw ChallengeStillShowing(state.url)
                return
            }
            delay(PollInterval)
        }
    }

    private suspend fun read(eval: suspend (String) -> String): ChallengeState = try {
        val json = decodePageAnswer(eval(STATE_SCRIPT))
        ChallengeState(json.flag("challenged"), json.flag("complete"), json.string("url").orEmpty())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ChallengeState.Navigating
    }

    private const val STATE_SCRIPT = """
        (function () {
          var challenged = false;
          try {
            // The global set by the Cloudflare beacon also exists on cleared pages, so it proves nothing.
            challenged = /^just a moment/i.test(document.title || '')
              || location.href.indexOf('__cf_chl_') !== -1
              || !!document.querySelector('script[src*="challenge-platform/h/"]');
          } catch (e) {}
          var payload = JSON.stringify({
            challenged: challenged,
            complete: document.readyState === 'complete',
            url: location.href
          });
          return btoa(unescape(encodeURIComponent(payload)));
        })()
    """
}

internal class ChallengeState(val challenged: Boolean, val complete: Boolean, val url: String) {
    val cleared: Boolean get() = !challenged && complete

    companion object {
        // A page in the middle of moving cannot answer, which means a check is moving on.
        val Navigating = ChallengeState(challenged = true, complete = false, url = "")
    }
}

// How long a bot check is given to pass on its own.
val ChallengeSettleBudget: Duration = 15.seconds

// A bot check is still showing after the browser was given time to pass it.
class ChallengeStillShowing(val url: String) : Exception("A bot check is still showing at $url")
