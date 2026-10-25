package com.sumizuri.app.feature.library.shelves

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.data.model.library.shelf.ShelfCondition

// Everything the dialog of a shelf can change, handed over together when it is saved.
internal class ShelfEdit(
    val name: String,
    val exclude: Boolean,
    val kind: Kind?,
    val rule: SortRule?,
    val status: Status?,
    val conditions: List<ShelfCondition>,
    val matchAll: Boolean,
)

// What the page of the shelves can ask for.
internal class ShelfCalls(
    val onAdd: (String, Kind?) -> Unit,
    val onDelete: (Long) -> Unit,
    val onReorder: (List<Long>) -> Unit,
    val onSave: (Category, ShelfEdit) -> Unit,
    // How many titles meet some conditions at this moment, for the editor to say.
    val countMatches: (List<ShelfCondition>, Boolean) -> Int,
)
