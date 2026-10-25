package com.sumizuri.app.data.model.library

import com.sumizuri.app.data.text.enumNamed
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.badge_favorite
import com.sumizuri.app.shared.resources.badge_language
import com.sumizuri.app.shared.resources.badge_source
import com.sumizuri.app.shared.resources.badge_status
import com.sumizuri.app.shared.resources.badge_unread
import com.sumizuri.app.shared.resources.corner_bottom_end
import com.sumizuri.app.shared.resources.corner_bottom_start
import com.sumizuri.app.shared.resources.corner_off
import com.sumizuri.app.shared.resources.corner_top_end
import com.sumizuri.app.shared.resources.corner_top_start
import org.jetbrains.compose.resources.StringResource

// A small mark that can sit on a cover.
enum class CoverBadgeKind(val label: StringResource) {
    Unread(Res.string.badge_unread),
    Status(Res.string.badge_status),
    Source(Res.string.badge_source),
    Language(Res.string.badge_language),
    Favorite(Res.string.badge_favorite),
}

// Where a mark sits on a cover, or that it is left off.
enum class CoverCorner(val label: StringResource) {
    Off(Res.string.corner_off),
    TopStart(Res.string.corner_top_start),
    TopEnd(Res.string.corner_top_end),
    BottomStart(Res.string.corner_bottom_start),
    BottomEnd(Res.string.corner_bottom_end),
}

// The corner of each mark. A mark that was never placed has its usual corner.
data class CoverBadgeLayout(private val placed: Map<CoverBadgeKind, CoverCorner> = emptyMap()) {
    fun cornerOf(kind: CoverBadgeKind): CoverCorner = placed[kind] ?: Usual.getValue(kind)

    fun with(kind: CoverBadgeKind, corner: CoverCorner): CoverBadgeLayout = copy(placed = placed + (kind to corner))

    // The marks that sit in one corner, in the order they are listed.
    fun at(corner: CoverCorner): List<CoverBadgeKind> = CoverBadgeKind.entries.filter { cornerOf(it) == corner }

    fun toText(): String = placed.entries.joinToString(",") { "${it.key.name}:${it.value.name}" }

    companion object {
        private val Usual = mapOf(
            CoverBadgeKind.Unread to CoverCorner.TopEnd,
            CoverBadgeKind.Status to CoverCorner.Off,
            CoverBadgeKind.Source to CoverCorner.Off,
            CoverBadgeKind.Language to CoverCorner.Off,
            CoverBadgeKind.Favorite to CoverCorner.TopStart,
        )

        fun parse(text: String?): CoverBadgeLayout = CoverBadgeLayout(
            text
                .orEmpty()
                .split(",")
                .mapNotNull { pair ->
                    val kind = enumNamed<CoverBadgeKind>(pair.substringBefore(":")) ?: return@mapNotNull null
                    val corner = enumNamed<CoverCorner>(pair.substringAfter(":")) ?: return@mapNotNull null
                    kind to corner
                }.toMap(),
        )
    }
}
