package com.sumizuri.app.data.settings

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.setting.SettingEntity
import com.sumizuri.app.data.model.library.CoverBadgeLayout
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.data.model.library.LibrarySortField
import com.sumizuri.app.data.model.library.QuickFilter
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.data.text.enumNamed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val Prefix = "library."

// How the library is set, kept as a setting row for each choice. A choice that was never made has its usual value.
class LibraryPrefsRepository(private val db: AppDatabase) {
    val prefs: Flow<LibraryPrefs> = db.settings().observeWithPrefix(Prefix).distinctUntilChanged().map { rows ->
        val saved = SavedChoices(rows.associate { it.name.removePrefix(Prefix) to it.value })
        val usual = LibraryPrefs()
        fun count(name: String, fallback: Int) =
            saved[name]?.toIntOrNull()?.takeIf { it in LibraryPrefs.Counts } ?: fallback
        LibraryPrefs(
            sort = saved.choice("sort", usual.sort),
            ascending = saved.flag("ascending", usual.ascending),
            thenSort = enumNamed<LibrarySortField>(saved["thenSort"]),
            thenAscending = saved.flag("thenAscending", usual.thenAscending),
            rules = saved.rows
                .filterKeys { it.startsWith("rule.") }
                .mapNotNull { (name, text) -> SortRule.decode(text)?.let { name.removePrefix("rule.") to it } }
                .toMap(),
            pinFavorites = saved.flag("pinFavorites", usual.pinFavorites),
            pinUnread = saved.flag("pinUnread", usual.pinUnread),
            category = saved["category"]?.toLongOrNull(),
            filters = saved["filters"]
                .orEmpty()
                .split(",")
                .mapNotNull { name -> enumNamed<QuickFilter>(name) }
                .toSet(),
            style = saved.choice("style", usual.style),
            cellWidth = saved.whole("cellWidth", usual.cellWidth, LibraryPrefs.CellRange),
            gridGap = saved.whole("gridGap", usual.gridGap, LibraryPrefs.GapRange),
            panoramaAspect = saved.fraction("panoramaAspect", usual.panoramaAspect, LibraryPrefs.PanoramaRange),
            listCover = saved.whole("listCover", usual.listCover, LibraryPrefs.ListCoverRange),
            homeCover = saved.whole("homeCover", usual.homeCover, LibraryPrefs.HomeCoverRange),
            nameScale = saved.fraction("nameScale", usual.nameScale, LibraryPrefs.NameScaleRange),
            badges = CoverBadgeLayout.parse(saved["badges"]),
            showProgress = saved.flag("showProgress", usual.showProgress),
            favoritesEnabled = saved.flag("favoritesEnabled", usual.favoritesEnabled),
            hideAllTab = saved.flag("hideAllTab", usual.hideAllTab),
            hideNoShelfTab = saved.flag("hideNoShelfTab", usual.hideNoShelfTab),
            continueCount = count("continueCount", usual.continueCount),
            updatesCount = count("updatesCount", usual.updatesCount),
            historyCount = count("historyCount", usual.historyCount),
            continueStyle = saved.choice("continueStyle", usual.continueStyle),
            updatesStyle = saved.choice("updatesStyle", usual.updatesStyle),
            historyStyle = saved.choice("historyStyle", usual.historyStyle),
            sectionOrder = sectionOrder(saved["sectionOrder"]),
            collapsibleSearch = saved.flag("collapsibleSearch", usual.collapsibleSearch),
            unreachableDismissed = saved["unreachableDismissed"]?.toIntOrNull() ?: usual.unreachableDismissed,
        )
    }

    // The saved order, with a part that is missing from it put at the end.
    private fun sectionOrder(text: String?): List<LibrarySection> {
        val saved = text
            .orEmpty()
            .split(",")
            .mapNotNull { name -> enumNamed<LibrarySection>(name) }
            .distinct()
        return saved + LibrarySection.entries.filter { it !in saved }
    }

    // Applies a change to the choices as they are now, and saves what the change made different.
    suspend fun update(change: (LibraryPrefs) -> LibraryPrefs) {
        val before = prefs.first()
        val after = change(before)
        if (after.sort != before.sort) put("sort", after.sort.name)
        if (after.ascending != before.ascending) put("ascending", after.ascending.toString())
        if (after.thenSort !=
            before.thenSort
        ) {
            after.thenSort?.let { put("thenSort", it.name) }
                ?: db.settings().delete(Prefix + "thenSort")
        }
        if (after.thenAscending != before.thenAscending) put("thenAscending", after.thenAscending.toString())
        for ((scope, rule) in after.rules) if (before.rules[scope] != rule) put("rule.$scope", rule.encode())
        for (scope in before.rules.keys - after.rules.keys) db.settings().delete(Prefix + "rule." + scope)
        if (after.pinFavorites != before.pinFavorites) put("pinFavorites", after.pinFavorites.toString())
        if (after.pinUnread != before.pinUnread) put("pinUnread", after.pinUnread.toString())
        if (after.category !=
            before.category
        ) {
            after.category?.let { put("category", it.toString()) }
                ?: db.settings().delete(Prefix + "category")
        }
        if (after.filters != before.filters) put("filters", after.filters.joinToString(",") { it.name })
        if (after.style != before.style) put("style", after.style.name)
        if (after.cellWidth != before.cellWidth) put("cellWidth", after.cellWidth.toString())
        if (after.gridGap != before.gridGap) put("gridGap", after.gridGap.toString())
        if (after.panoramaAspect != before.panoramaAspect) put("panoramaAspect", after.panoramaAspect.toString())
        if (after.listCover != before.listCover) put("listCover", after.listCover.toString())
        if (after.homeCover != before.homeCover) put("homeCover", after.homeCover.toString())
        if (after.nameScale != before.nameScale) put("nameScale", after.nameScale.toString())
        if (after.badges != before.badges) put("badges", after.badges.toText())
        if (after.showProgress != before.showProgress) put("showProgress", after.showProgress.toString())
        if (after.favoritesEnabled !=
            before.favoritesEnabled
        ) {
            put("favoritesEnabled", after.favoritesEnabled.toString())
        }
        if (after.hideAllTab != before.hideAllTab) put("hideAllTab", after.hideAllTab.toString())
        if (after.hideNoShelfTab != before.hideNoShelfTab) put("hideNoShelfTab", after.hideNoShelfTab.toString())
        if (after.continueCount != before.continueCount) put("continueCount", after.continueCount.toString())
        if (after.updatesCount != before.updatesCount) put("updatesCount", after.updatesCount.toString())
        if (after.historyCount != before.historyCount) put("historyCount", after.historyCount.toString())
        if (after.continueStyle != before.continueStyle) put("continueStyle", after.continueStyle.name)
        if (after.updatesStyle != before.updatesStyle) put("updatesStyle", after.updatesStyle.name)
        if (after.historyStyle != before.historyStyle) put("historyStyle", after.historyStyle.name)
        if (after.sectionOrder !=
            before.sectionOrder
        ) {
            put("sectionOrder", after.sectionOrder.joinToString(",") { it.name })
        }
        if (after.collapsibleSearch !=
            before.collapsibleSearch
        ) {
            put("collapsibleSearch", after.collapsibleSearch.toString())
        }
        if (after.unreachableDismissed !=
            before.unreachableDismissed
        ) {
            put("unreachableDismissed", after.unreachableDismissed.toString())
        }
    }

    private suspend fun put(name: String, value: String) = db.settings().put(SettingEntity(Prefix + name, value))
}
