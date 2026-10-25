package com.sumizuri.app.source.browser.request

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RenderResult(
    val html: String,
    val url: String,
    val resources: List<String> = emptyList(),
    val result: String? = null,
    // True when what was waited for never showed up. The page is still returned so the reason can be seen.
    val timedOut: Boolean = false,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("html", html)
        put("url", url)
        put("resources", buildJsonArray { resources.forEach { add(JsonPrimitive(it)) } })
        put("result", result?.let { JsonPrimitive(it) } ?: JsonNull)
        put("timedOut", timedOut)
    }
}

val DefaultRenderTimeout: Duration = 20.seconds

val MaxRenderTimeout: Duration = 85.seconds
