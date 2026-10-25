package com.sumizuri.app.data.model.library

// How the person has the library set: the order, the shelf, the quick filters, and the look of the page.
data class LibraryPrefs(
    val sort: LibrarySortField = LibrarySortField.Title,
    val ascending: Boolean = true,
    // The field that settles ties of the default order, and its direction.
    val thenSort: LibrarySortField? = null,
    val thenAscending: Boolean = true,
    // Orders of their own for the tab of all the titles, the tab of titles on no shelf and each kind, by the name of the place, which are all, none and kind followed by a dot and the code of the kind.
    val rules: Map<String, SortRule> = emptyMap(),
    // Favourites and titles with something unread come first, whatever the order is.
    val pinFavorites: Boolean = false,
    val pinUnread: Boolean = false,
    // The shelf that is open, or null for all the titles.
    val category: Long? = null,
    val filters: Set<QuickFilter> = emptySet(),
    val style: LibraryDisplayStyle = LibraryDisplayStyle.ComfortableGrid,
    // No cell of the grid is wider than this, in dp. A smaller number gives more and smaller covers.
    val cellWidth: Int = DefaultCellWidth,
    // The space between the covers of a grid, in dp.
    val gridGap: Int = DefaultGap,
    // How many times wider than high a tile of the wide grid is.
    val panoramaAspect: Float = DefaultPanorama,
    // How wide the cover of a row of the list is, in dp. The thin list takes a smaller one in proportion.
    val listCover: Int = DefaultListCover,
    // How wide a cover is in the parts above the library, in dp.
    val homeCover: Int = DefaultHomeCover,
    // How big the names are, as a part of the usual size.
    val nameScale: Float = 1f,
    val badges: CoverBadgeLayout = CoverBadgeLayout(),
    // A thin bar over the bottom of a cover that shows how far the reader is through the title.
    val showProgress: Boolean = false,
    // Whether titles can be marked as favourites.
    val favoritesEnabled: Boolean = true,
    // The tabs for all the titles and for the titles on no shelf, which can be taken away.
    val hideAllTab: Boolean = false,
    val hideNoShelfTab: Boolean = false,
    // How many titles the parts above the covers hold, with 0 for no part.
    val continueCount: Int = 8,
    val updatesCount: Int = 0,
    val historyCount: Int = 0,
    // The titles to go on with are low cards, so the part takes little of the page above the covers.
    val continueStyle: ShelfStyle = ShelfStyle.Row,
    val updatesStyle: ShelfStyle = ShelfStyle.Row,
    val historyStyle: ShelfStyle = ShelfStyle.Row,
    // The order of those parts, top to bottom.
    val sectionOrder: List<LibrarySection> = LibrarySection.entries,
    // The search shows only when its button in the bar is pressed.
    val collapsibleSearch: Boolean = false,
    // How many unreachable titles there were when the person closed the note about them, so it comes back only when more are lost.
    val unreachableDismissed: Int = 0,
) {
    // The order used where no place has one of its own.
    val defaultRule: SortRule get() = SortRule(sort, ascending, thenSort, thenAscending)

    // The Updates and History pages are tabs only while their part of the library is off. With the part on, the page is reached from the part and its tab leaves the navigation.
    val showUpdatesTab: Boolean get() = updatesCount == 0
    val showHistoryTab: Boolean get() = historyCount == 0

    // How many titles a part shows, with 0 for a part that is off.
    fun countOf(section: LibrarySection): Int = when (section) {
        LibrarySection.Continue -> continueCount
        LibrarySection.Updates -> updatesCount
        LibrarySection.History -> historyCount
        LibrarySection.Library -> 0
    }

    fun withCount(section: LibrarySection, count: Int): LibraryPrefs = when (section) {
        LibrarySection.Continue -> copy(continueCount = count)
        LibrarySection.Updates -> copy(updatesCount = count)
        LibrarySection.History -> copy(historyCount = count)
        LibrarySection.Library -> this
    }

    fun withStyle(section: LibrarySection, style: ShelfStyle): LibraryPrefs = when (section) {
        LibrarySection.Continue -> copy(continueStyle = style)
        LibrarySection.Updates -> copy(updatesStyle = style)
        LibrarySection.History -> copy(historyStyle = style)
        LibrarySection.Library -> this
    }

    fun styleOf(section: LibrarySection): ShelfStyle = when (section) {
        LibrarySection.Continue -> continueStyle
        LibrarySection.Updates -> updatesStyle
        LibrarySection.History -> historyStyle
        LibrarySection.Library -> ShelfStyle.Row
    }

    companion object {
        const val DefaultCellWidth = 156
        val CellRange = 70f..320f
        const val DefaultGap = 12
        val GapRange = 2f..28f
        const val DefaultPanorama = 16f / 9f
        val PanoramaRange = 1.3f..3f
        const val DefaultListCover = 52
        val ListCoverRange = 36f..110f
        const val DefaultHomeCover = 108
        val HomeCoverRange = 70f..200f
        val NameScaleRange = 0.8f..1.5f
        val Counts = listOf(0, 3, 5, 8, 10, 15, 20)

        // What a part shows when it is switched on again.
        const val ShownCount = 10
    }
}
