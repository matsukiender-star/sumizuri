package com.sumizuri.app.data.model.controls

import com.sumizuri.app.data.text.enumNamed
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.put

// The reader controls as text, for the database and for a file the person keeps.
fun ReaderControls.toJson(): String = buildJsonObject {
    put("format", 1)
    put("keys", keysToJson(keys))
    put(
        "tapZones",
        buildJsonObject {
            for ((layout, zones) in tapZones) {
                put(
                    layout.name,
                    buildJsonArray {
                        zones.forEach {
                            add(
                                if (it ==
                                    null
                                ) {
                                    JsonNull
                                } else {
                                    JsonPrimitive(it.name)
                                },
                            )
                        }
                    },
                )
            }
        },
    )
    put(
        "scroll",
        buildJsonObject {
            put("arrowDp", scroll.arrowDp)
            put("pageFraction", scroll.pageFraction)
            put("tapFraction", scroll.tapFraction)
        },
    )
    if (volumeKeys) put("volumeKeys", true)
    if (volumeInverted) put("volumeInverted", true)
    if (!longTapMenu) put("longTapMenu", false)
    if (geometry.isNotEmpty()) {
        put(
            "geometry",
            buildJsonObject {
                for ((layout, sizes) in geometry) {
                    put(
                        layout.name,
                        buildJsonObject {
                            put("x", sizes.edgeX)
                            put("y", sizes.edgeY)
                        },
                    )
                }
            },
        )
    }
}.toString()

fun readerControlsOf(text: String?): ReaderControls {
    val root =
        text?.takeIf { it.isNotBlank() }?.let { runCatching { Json.parseToJsonElement(it) as? JsonObject }.getOrNull() }
            ?: return ReaderControls()
    val zones = ReaderControls.DefaultZones.toMutableMap()
    (root["tapZones"] as? JsonObject)?.let { all ->
        for (layout in TapLayout.entries) {
            val list = all[layout.name] as? JsonArray ?: continue
            if (list.size != ZoneCount) continue
            zones[layout] = list.map { cell -> enumNamed<ReaderAction>((cell as? JsonPrimitive)?.contentOrNull) }
        }
    }
    val scrollObj = root["scroll"] as? JsonObject
    fun number(name: String, fallback: Float, range: ClosedFloatingPointRange<Float>) = (
        (scrollObj?.get(name) as? JsonPrimitive)?.floatOrNull
            ?: fallback
        ).coerceIn(range)
    val usual = ScrollSteps()
    val geometry = TapLayout.entries
        .mapNotNull { layout ->
            val sizes = (root["geometry"] as? JsonObject)?.get(layout.name) as? JsonObject ?: return@mapNotNull null
            fun edge(name: String) = ((sizes[name] as? JsonPrimitive)?.floatOrNull ?: TapGeometry().edgeX).coerceIn(
                TapGeometry.Smallest,
                TapGeometry.Largest,
            )
            layout to TapGeometry(edge("x"), edge("y"))
        }.toMap()
    return ReaderControls(
        keys = keysOf(root["keys"], ReaderAction.entries, ReaderControls.DefaultKeys),
        tapZones = zones,
        scroll = ScrollSteps(
            number("arrowDp", usual.arrowDp, ScrollSteps.ArrowRange.start..ScrollSteps.ArrowRange.endInclusive),
            number(
                "pageFraction",
                usual.pageFraction,
                ScrollSteps.FractionRange.start..ScrollSteps.FractionRange.endInclusive,
            ),
            number(
                "tapFraction",
                usual.tapFraction,
                ScrollSteps.FractionRange.start..ScrollSteps.FractionRange.endInclusive,
            ),
        ),
        geometry = geometry,
        volumeKeys = (root["volumeKeys"] as? JsonPrimitive)?.booleanOrNull ?: false,
        volumeInverted = (root["volumeInverted"] as? JsonPrimitive)?.booleanOrNull ?: false,
        longTapMenu = (root["longTapMenu"] as? JsonPrimitive)?.booleanOrNull ?: true,
    )
}
