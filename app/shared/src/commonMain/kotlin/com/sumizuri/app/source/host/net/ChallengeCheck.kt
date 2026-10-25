package com.sumizuri.app.source.host.net

import com.sumizuri.app.source.host.ChallengeException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Tells a bot check page, such as Cloudflare, from the page that was asked for.
internal object ChallengeCheck {
    // Found on a challenge page only, whatever its status.
    private val interstitialMarkers = listOf(
        "_cf_chl_opt",
        "<title>just a moment",
        "challenge-platform/h/",
        "cf-browser-verification",
    )

    // Wording of older challenge and block pages, only trusted on an error status.
    private val blockPageMarkers = listOf(
        "attention required! | cloudflare",
        "enable javascript and cookies to continue",
        "checking your browser before accessing",
    )

    fun reason(status: Int, headers: JsonObject, body: String): String? {
        for ((name, value) in headers) {
            val text = (value as? JsonPrimitive)?.content.orEmpty()
            if (name.equals("cf-mitigated", ignoreCase = true) && text.lowercase().contains("challenge")) {
                return "cf-mitigated: $text"
            }
        }
        val lower = body.lowercase()
        interstitialMarkers.firstOrNull { lower.contains(it) }?.let { return "status $status, \"$it\"" }
        if (status == 403 || status == 503) {
            blockPageMarkers.firstOrNull { lower.contains(it) }?.let { return "status $status, \"$it\"" }
        }
        return null
    }

    fun throwIfChallenge(url: String, status: Int, headers: JsonObject, body: String) {
        val why = reason(status, headers, body) ?: return
        throw ChallengeException(url.trim(), "$why; body starts: ${body.take(200)}")
    }
}
