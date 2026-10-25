package com.sumizuri.app.data.blocks

import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.stats.ReadingStats
import com.sumizuri.app.data.model.stats.ReadingStats.Companion.DayMs
import com.sumizuri.app.data.text.localOffsetMillis
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// The blocks of the profile page in the order they are laid out.
@Serializable
data class BlockLayout(val version: Int = 2, val blocks: List<ProfileBlock> = emptyList())

// One block of the profile page: its script, which is JSON with formulas in it, and its size. It is w columns of four wide, and h rows high, where 0 lets it take the height of what it shows. A block saved by the first version of blocks held a graph of nodes in place of a script, which is only read to bring it over.
@Serializable
data class ProfileBlock(
    val id: String,
    val w: Int = 4,
    val h: Int = 0,
    val script: String = "",
    val graph: JsonElement? = null,
)

// What the blocks work from: the chapters read, the titles of the library, the statistics worked out from them, and the time.
class BlockContext(val reads: List<ReadEvent>, val titles: List<Title>, val stats: ReadingStats, val now: Long) {
    val titleById: Map<Long, Title> by lazy { titles.associateBy { it.id } }

    // The offset of the time zone changes only a few times a year, so it is asked once for each hour that has reads. The map is made once and then only read, so blocks may run on it side by side.
    private val offsets: Map<Long, Long> by lazy {
        reads.map { it.at.floorDiv(HourMs) }.distinct().associateWith { localOffsetMillis(it * HourMs) }
    }

    // A time on the clock of the device, as epoch milliseconds read as if it were UTC.
    fun local(at: Long): Long = at + (offsets[at.floorDiv(HourMs)] ?: localOffsetMillis(at))

    // The day a time falls on, counted from 1970 on the clock of the device.
    fun dayOf(at: Long): Long = local(at).floorDiv(DayMs)

    // The hour of the day a time falls on, from 0 to 23.
    fun hourOf(at: Long): Int = (local(at).mod(DayMs) / HourMs).toInt()

    val today: Long by lazy { dayOf(now) }
}

internal const val HourMs = 60 * 60 * 1000L
