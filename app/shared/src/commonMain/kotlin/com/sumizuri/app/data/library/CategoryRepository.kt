package com.sumizuri.app.data.library

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.category.CategoryEntity
import com.sumizuri.app.data.db.category.TitleCategoryEntity
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibrarySortField
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.data.model.library.shelf.ShelfCondition
import com.sumizuri.app.data.model.library.shelf.ShelfConditions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Ids are sent to the database in parts, since a query takes only so many.
private const val Chunk = 500

// The shelves of the library: making them, renaming, ordering and removing them, and putting titles on them.
class CategoryRepository(private val db: AppDatabase) {
    val categories: Flow<List<Category>> =
        combine(db.categories().observeAll(), db.categories().observeCounts()) { rows, counts ->
            val sizes = counts.associate { it.categoryId to it.titles }
            rows.map { row ->
                Category(
                    row.id, row.name, row.excludeFromUpdate, sizes[row.id] ?: 0,
                    sort = row.sortField?.let { LibrarySortField.entries.getOrNull(it) },
                    ascending = row.sortAscending,
                    then = row.thenField?.let { LibrarySortField.entries.getOrNull(it) },
                    thenAscending = row.thenAscending,
                    status = row.statusFilter.takeIf { it != 0 }?.let { Status.fromCode(it) },
                    kind = Kind.entries.firstOrNull { it.code == row.kind },
                    conditions = ShelfConditions.decode(row.conditions),
                    matchAll = row.matchAll,
                )
            }
        }

    // The shelves each title is on.
    val memberships: Flow<Map<Long, Set<Long>>> = db.categories().observeMemberships().map { rows ->
        rows.groupBy({ it.titleId }, { it.categoryId }).mapValues { it.value.toSet() }
    }

    // Makes a shelf at the end for a kind of title, or for every kind when none is given, and gives its id, or null when the name is empty or a shelf of that kind has it already.
    suspend fun add(name: String, kind: Kind? = null): Long? {
        val clean = name.trim()
        val code = kind?.code ?: -1
        if (clean.isEmpty() ||
            db.categories().all().any { it.kind == code && it.name.equals(clean, ignoreCase = true) }
        ) {
            return null
        }
        return db.categories().insert(
            CategoryEntity(name = clean, sortOrder = db.categories().lastOrder() + 1, kind = code),
        )
    }

    // Makes a shelf be for another kind of title, or for every kind with null. Nothing happens when a shelf of that kind has its name already.
    suspend fun setKind(id: Long, kind: Kind?) {
        val code = kind?.code ?: -1
        val shelf = db.categories().all().firstOrNull { it.id == id } ?: return
        if (db.categories().all().any {
                it.id != id && it.kind == code && it.name.equals(shelf.name, ignoreCase = true)
            }
        ) {
            return
        }
        db.categories().setKind(id, code)
    }

    suspend fun rename(id: Long, name: String) {
        val clean = name.trim()
        val mine = db.categories().all().firstOrNull { it.id == id } ?: return
        if (clean.isEmpty() ||
            db.categories().all().any {
                it.id != id && it.kind == mine.kind && it.name.equals(
                    clean,
                    ignoreCase = true,
                )
            }
        ) {
            return
        }
        db.categories().rename(id, clean)
    }

    suspend fun delete(id: Long) = db.categories().delete(id)

    // What a shelf is ordered by and which status it shows, with null for the order of the library and for any status.
    suspend fun setRule(id: Long, rule: SortRule?, status: Status?) = db.categories().setRule(
        id,
        rule?.sort?.ordinal,
        rule?.ascending ?: true,
        rule?.then?.ordinal,
        rule?.thenAscending ?: true,
        status?.code ?: 0,
    )

    // Sets what puts titles on a shelf by themselves.
    suspend fun setConditions(id: Long, conditions: List<ShelfCondition>, matchAll: Boolean) =
        db.categories().setConditions(id, ShelfConditions.encode(conditions), matchAll)

    suspend fun setExcluded(id: Long, exclude: Boolean) = db.categories().setExcluded(id, exclude)

    // Puts the shelves in the order of the ids.
    suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { order, shelf -> db.categories().setOrder(shelf, order) }
    }

    suspend fun categoriesOf(titleId: Long): Set<Long> = db.categories().categoriesOf(titleId).toSet()

    // Puts the titles on the shelves to add and takes them off the shelves to remove.
    suspend fun change(titleIds: Collection<Long>, add: Set<Long>, remove: Set<Long>) {
        for (part in titleIds.chunked(Chunk)) {
            if (remove.isNotEmpty()) db.categories().removeFrom(part, remove.toList())
            if (add.isNotEmpty()) {
                db.categories().addAll(
                    part.flatMap { title -> add.map { TitleCategoryEntity(title, it) } },
                )
            }
        }
    }
}
