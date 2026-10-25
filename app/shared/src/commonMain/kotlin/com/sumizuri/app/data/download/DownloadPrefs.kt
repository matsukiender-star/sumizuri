package com.sumizuri.app.data.download

import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The choices of saving chapters to the device.
data class DownloadPrefs(
    // Chapters are only saved on Wi-Fi or another connection that is not metered.
    val wifiOnly: Boolean = false,
    // How many pages of a chapter are fetched at the same time.
    val pagesAtOnce: Int = 3,
    // The new chapters a check of the library finds are saved.
    val newChapters: Boolean = false,
    // Only for titles with a chapter read, so titles kept for later do not fill the device.
    val newOnlyStarted: Boolean = true,
    // The most new chapters saved for one title at one check, 0 for all of them.
    val newLimit: Int = 0,
    // The shelves whose titles get no new chapters saved.
    val skipShelves: Set<Long> = emptySet(),
    // While reading, how many chapters after the open one are saved, 0 for none.
    val ahead: Int = 0,
    // Saving ahead only starts when the open chapter is saved itself, so reading online does not start downloads.
    val aheadOnlySaved: Boolean = true,
    // How many read chapters behind the last one finished stay on the device, or -1 to keep every one.
    val deleteAfter: Int = -1,
    // Bookmarked chapters are never taken off the device on their own.
    val keepBookmarked: Boolean = true,
    // Marking chapters read by hand takes them off the device too.
    val deleteOnMark: Boolean = false,
)

// The choices of how many pages are fetched at once, how many chapters are saved ahead, and how many read ones are kept.
val PagesAtOnceChoices = listOf(1, 2, 3, 4, 6)
val NewLimitChoices = listOf(0, 1, 2, 3, 5, 10)
val AheadChoices = listOf(0, 1, 2, 3, 5, 10)
val DeleteAfterChoices = listOf(-1, 0, 1, 2, 3, 5)

// The ids of shelves as they are kept: numbers with commas between.
fun shelfSet(text: String?): Set<Long> = text.orEmpty().split(',').mapNotNullTo(HashSet()) { it.trim().toLongOrNull() }

fun shelfText(ids: Set<Long>): String = ids.sorted().joinToString(",")

fun SettingsRepository.downloadPrefs(): Flow<DownloadPrefs> = prefixed(SettingKey.DownloadPrefix).map { saved ->
    fun flag(key: String, fallback: Boolean) = saved[key]?.toBooleanStrictOrNull() ?: fallback
    fun number(key: String, fallback: Int) = saved[key]?.toIntOrNull() ?: fallback
    val usual = DownloadPrefs()
    DownloadPrefs(
        wifiOnly = flag(SettingKey.DownloadWifiOnly, usual.wifiOnly),
        pagesAtOnce = number(SettingKey.DownloadPagesAtOnce, usual.pagesAtOnce).coerceIn(1, 8),
        newChapters = flag(SettingKey.DownloadNew, usual.newChapters),
        newOnlyStarted = flag(SettingKey.DownloadNewStarted, usual.newOnlyStarted),
        newLimit = number(SettingKey.DownloadNewLimit, usual.newLimit).coerceAtLeast(0),
        skipShelves = shelfSet(saved[SettingKey.DownloadSkipShelves]),
        ahead = number(SettingKey.DownloadAhead, usual.ahead).coerceAtLeast(0),
        aheadOnlySaved = flag(SettingKey.DownloadAheadSaved, usual.aheadOnlySaved),
        deleteAfter = number(SettingKey.DownloadDeleteAfter, usual.deleteAfter).coerceAtLeast(-1),
        keepBookmarked = flag(SettingKey.DownloadKeepBookmarked, usual.keepBookmarked),
        deleteOnMark = flag(SettingKey.DownloadDeleteOnMark, usual.deleteOnMark),
    )
}
