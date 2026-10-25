package com.sumizuri.app.data.settings

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.setting.SettingEntity

private fun key(titleId: Long) = "cover.focus.$titleId"

// The spot a person picked on a cover for the backdrop to centre on, kept for each title. Without one, the page looks for the face by itself.
class CoverFocusRepository(private val db: AppDatabase) {
    // The spot as fractions of the width and the height of the cover, or null when none was picked.
    suspend fun get(titleId: Long): Pair<Float, Float>? {
        val parts = db.settings().get(key(titleId))?.split(",") ?: return null
        val x = parts.getOrNull(0)?.toFloatOrNull() ?: return null
        val y = parts.getOrNull(1)?.toFloatOrNull() ?: return null
        return x to y
    }

    suspend fun set(titleId: Long, x: Float, y: Float) = db.settings().put(SettingEntity(key(titleId), "$x,$y"))

    suspend fun clear(titleId: Long) = db.settings().delete(key(titleId))

    // Gives a title the spot another title had, for a title that moved, unless it has a spot of its own. With forget, the other title loses its spot.
    suspend fun carryOver(from: Long, to: Long, forget: Boolean) {
        val spot = db.settings().get(key(from)) ?: return
        if (db.settings().get(key(to)) == null) db.settings().put(SettingEntity(key(to), spot))
        if (forget) clear(from)
    }
}
