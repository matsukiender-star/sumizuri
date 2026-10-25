package com.sumizuri.app.source.browser.request

import com.sumizuri.app.source.host.BridgeException
import com.sumizuri.app.source.host.net.AddressGuard
import com.sumizuri.app.source.host.string
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RenderRequest(
    val url: String,
    val waitFor: String? = null,
    val waitForResource: String? = null,
    val capture: String? = null,
    val script: String? = null,
    // Runs before the page's own scripts and answers through the same result as script.
    val early: String? = null,
    val userAgent: String? = null,
    val timeout: Duration = DefaultRenderTimeout,
    val settle: Duration = 300.milliseconds,
) {
    companion object {
        fun fromJson(json: JsonObject): RenderRequest {
            val url = json.string("url") ?: throw BridgeException("host.render needs a url")
            return RenderRequest(
                url = AddressGuard.check(url),
                waitFor = text(json, "waitFor"),
                waitForResource = pattern(json, "waitForResource"),
                capture = pattern(json, "capture"),
                script = text(json, "script"),
                early = text(json, "early"),
                userAgent = text(json, "userAgent"),
                timeout = minOf(timeout(json), MaxRenderTimeout),
            )
        }

        private fun text(json: JsonObject, key: String): String? {
            val value = json[key] ?: return null
            if (value is JsonNull) return null
            val content = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (content.isNullOrBlank()) throw BridgeException("host.render: \"$key\" must be a non empty string")
            return content
        }

        private fun pattern(json: JsonObject, key: String): String? {
            val value = text(json, key) ?: return null
            try {
                Regex(value)
            } catch (error: IllegalArgumentException) {
                throw BridgeException("host.render: \"$key\" is not a valid pattern: ${error.message}")
            }
            return value
        }

        private fun timeout(json: JsonObject): Duration {
            val value = json["timeout"]?.takeIf { it !is JsonNull } ?: return DefaultRenderTimeout
            val ms = (value as? JsonPrimitive)?.content?.toDoubleOrNull()
            if (ms == null ||
                ms <= 0
            ) {
                throw BridgeException("host.render: \"timeout\" must be a number of milliseconds")
            }
            return ms.toLong().milliseconds
        }
    }
}
