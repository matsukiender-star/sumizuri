package com.sumizuri.app.source.browser.page

import com.sumizuri.app.source.browser.ChallengeSettleBudget
import com.sumizuri.app.source.browser.ChallengeWaiter
import com.sumizuri.app.source.browser.request.RenderRequest
import com.sumizuri.app.source.browser.request.RenderResult
import com.sumizuri.app.source.host.string
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal object PageRenderer {
    private val PollInterval = 250.milliseconds

    // Loads the page, waits out a bot check, waits for what the request asks for, then reads the page.
    suspend fun render(
        page: RenderPage,
        request: RenderRequest,
        challengeBudget: Duration = ChallengeSettleBudget,
    ): RenderResult {
        val clock = TimeSource.Monotonic.markNow()
        request.early?.let { page.runFirst(earlyWrapper(it)) }
        try {
            withTimeout(request.timeout) { PageLoader.loadWithRetry(page, request.url) }
        } catch (error: TimeoutCancellationException) {
            throw PageLoadException("The page did not finish loading, waited ${request.timeout.inWholeSeconds} seconds")
        }
        ChallengeWaiter.settle(page, challengeBudget)
        val waitPattern = request.waitForResource?.let { Regex(it) }
        var timedOut = false
        while (true) {
            val state = probe(page, request.waitFor)
            val selectorReady = request.waitFor == null || state.hasSelector
            val resourceReady = waitPattern == null || state.resources.any { waitPattern.containsMatchIn(it) }
            if (state.complete && selectorReady && resourceReady) break
            if (clock.elapsedNow() >= request.timeout) {
                timedOut = true
                break
            }
            delay(PollInterval)
        }
        if (!timedOut) delay(request.settle)
        return read(page, request, timedOut)
    }

    private class State(val complete: Boolean, val hasSelector: Boolean, val resources: List<String>)

    private suspend fun probe(page: RenderPage, selector: String?): State {
        val lookup = if (selector == null) "true" else "!!document.querySelector(${quoteForScript(selector)})"
        val json = decodePageAnswer(
            page.eval(
                """
                (function () {
                  var found = false;
                  try { found = $lookup; } catch (e) { found = false; }
                  var payload = JSON.stringify({
                    complete: document.readyState === 'complete',
                    hasSelector: found,
                    resources: performance.getEntriesByType('resource').map(function (e) { return e.name; })
                  });
                  return btoa(unescape(encodeURIComponent(payload)));
                })()
                """.trimIndent(),
            ),
        )
        return State(json.flag("complete"), json.flag("hasSelector"), json.strings("resources"))
    }

    private suspend fun read(page: RenderPage, request: RenderRequest, timedOut: Boolean): RenderResult {
        // An early script is already running since the page started, so only its answer is waited for.
        val scriptResult = if (request.early !=
            null
        ) {
            awaitScript(page, request.timeout)
        } else {
            request.script?.let { runUserScript(page, it, request.timeout) }
        }
        val json = decodePageAnswer(
            page.eval(
                """
                (function () {
                  var payload = JSON.stringify({
                    html: document.documentElement.outerHTML,
                    url: location.href,
                    resources: performance.getEntriesByType('resource').map(function (e) { return e.name; })
                  });
                  return btoa(unescape(encodeURIComponent(payload)));
                })()
                """.trimIndent(),
            ),
        )
        val capture = request.capture?.let { Regex(it) }
        val seen = mutableSetOf<String>()
        return RenderResult(
            html = json.string("html").orEmpty(),
            url = json.string("url") ?: request.url,
            resources = if (capture ==
                null
            ) {
                emptyList()
            } else {
                json.strings("resources").filter { capture.containsMatchIn(it) && seen.add(it) }
            },
            result = scriptResult,
            timedOut = timedOut,
        )
    }

    // Runs the request's own script in the page and waits for its answer, or for an error.
    private suspend fun runUserScript(page: RenderPage, script: String, limit: Duration): String? {
        page.eval(
            """
            (function () {
              window.__sumizuriDone = false;
              window.__sumizuriResult = null;
              window.__sumizuriError = null;
              try {
                var val = (0, eval)(${quoteForScript(script)});
                if (val && typeof val.then === 'function') {
                  val.then(function (res) {
                    window.__sumizuriResult = res === undefined || res === null ? null : String(res);
                    window.__sumizuriDone = true;
                  }).catch(function (err) {
                    window.__sumizuriError = 'ERROR: ' + (err && err.message ? err.message : String(err));
                    window.__sumizuriDone = true;
                  });
                } else {
                  window.__sumizuriResult = val === undefined || val === null ? null : String(val);
                  window.__sumizuriDone = true;
                }
              } catch (e) {
                window.__sumizuriError = 'ERROR: ' + (e && e.message ? e.message : String(e));
                window.__sumizuriDone = true;
              }
            })()
            """.trimIndent(),
        )
        return awaitScript(page, limit)
    }

    // The early script with the two calls it answers through. It runs once in every new document of the page.
    private fun earlyWrapper(script: String): String =
        """
        (function () {
          if (window.top !== window || window.__sumizuriEarly) return;
          window.__sumizuriEarly = true;
          window.__sumizuriDone = false;
          window.__sumizuriResult = null;
          window.__sumizuriError = null;
          window.__reportResult = function (value) {
            if (window.__sumizuriDone) return;
            window.__sumizuriResult = JSON.stringify(value === undefined ? null : value);
            window.__sumizuriDone = true;
          };
          window.__reportError = function (message) {
            if (window.__sumizuriDone) return;
            window.__sumizuriError = 'ERROR: ' + String(message);
            window.__sumizuriDone = true;
          };
          try {
            (0, eval)(${quoteForScript(script)});
          } catch (e) {
            window.__reportError(e && e.message ? e.message : e);
          }
        })();
        """.trimIndent()

    // Waits for the script in the page to answer, or for an error.
    private suspend fun awaitScript(page: RenderPage, limit: Duration): String? {
        val clock = TimeSource.Monotonic.markNow()
        while (true) {
            val poll = decodePageAnswer(
                page.eval(
                    """
                    (function () {
                      var payload = JSON.stringify({
                        done: window.__sumizuriDone === true,
                        result: window.__sumizuriResult,
                        error: window.__sumizuriError
                      });
                      return btoa(unescape(encodeURIComponent(payload)));
                    })()
                    """.trimIndent(),
                ),
            )
            if (poll.flag("done")) return poll.string("error") ?: poll.string("result")
            if (clock.elapsedNow() >= limit) return "ERROR: script timed out"
            delay(100.milliseconds)
        }
    }
}
