package com.sumizuri.app.data.model.controls

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// The gestures and the shortcuts as text, in the same way.
fun AppGestures.toJson(): String = buildJsonObject {
    put("format", 1)
    put("libraryTap", libraryTap.name)
    put("libraryLongPress", libraryLongPress.name)
    put("libraryDoubleTap", libraryDoubleTap.name)
    put("chapterSwipeRight", chapterSwipeRight.name)
    put("chapterSwipeLeft", chapterSwipeLeft.name)
    put("chapterLongPress", chapterLongPress.name)
    put("navDoubleTap", navDoubleTap.name)
    put("shortcuts", keysToJson(shortcuts))
}.toString()

fun appGesturesOf(text: String?): AppGestures {
    val root =
        text?.takeIf { it.isNotBlank() }?.let { runCatching { Json.parseToJsonElement(it) as? JsonObject }.getOrNull() }
            ?: return AppGestures()
    val usual = AppGestures()
    return AppGestures(
        libraryTap = enumOf(LibraryCoverAction.entries, root["libraryTap"], usual.libraryTap),
        libraryLongPress = enumOf(LibraryCoverAction.entries, root["libraryLongPress"], usual.libraryLongPress),
        libraryDoubleTap = enumOf(LibraryCoverAction.entries, root["libraryDoubleTap"], usual.libraryDoubleTap),
        chapterSwipeRight = enumOf(ChapterRowAction.entries, root["chapterSwipeRight"], usual.chapterSwipeRight),
        chapterSwipeLeft = enumOf(ChapterRowAction.entries, root["chapterSwipeLeft"], usual.chapterSwipeLeft),
        chapterLongPress = enumOf(ChapterRowAction.entries, root["chapterLongPress"], usual.chapterLongPress),
        navDoubleTap = enumOf(NavDoubleTapAction.entries, root["navDoubleTap"], usual.navDoubleTap),
        shortcuts = keysOf(root["shortcuts"], AppShortcut.entries, AppGestures.DefaultShortcuts),
    )
}
