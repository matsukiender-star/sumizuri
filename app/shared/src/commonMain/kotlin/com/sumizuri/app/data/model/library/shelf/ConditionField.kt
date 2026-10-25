package com.sumizuri.app.data.model.library.shelf

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cond_added
import com.sumizuri.app.shared.resources.cond_author
import com.sumizuri.app.shared.resources.cond_chapters
import com.sumizuri.app.shared.resources.cond_favorite
import com.sumizuri.app.shared.resources.cond_genre
import com.sumizuri.app.shared.resources.cond_kind
import com.sumizuri.app.shared.resources.cond_last_read
import com.sumizuri.app.shared.resources.cond_op_contains
import com.sumizuri.app.shared.resources.cond_op_greater
import com.sumizuri.app.shared.resources.cond_op_is
import com.sumizuri.app.shared.resources.cond_op_is_not
import com.sumizuri.app.shared.resources.cond_op_less
import com.sumizuri.app.shared.resources.cond_op_not_contains
import com.sumizuri.app.shared.resources.cond_progress
import com.sumizuri.app.shared.resources.cond_rating
import com.sumizuri.app.shared.resources.cond_source
import com.sumizuri.app.shared.resources.cond_status
import com.sumizuri.app.shared.resources.cond_title
import com.sumizuri.app.shared.resources.cond_unread
import com.sumizuri.app.shared.resources.cond_updated
import org.jetbrains.compose.resources.StringResource

// What a condition looks at, and how its value is written: one of a list of choices, a number, text, or yes and no.
enum class ConditionField(val label: StringResource, val value: ValueKind, val ops: List<ConditionOp>) {
    Status(Res.string.cond_status, ValueKind.Choice, ConditionOp.Same),
    Kind(Res.string.cond_kind, ValueKind.Choice, ConditionOp.Same),
    Favorite(Res.string.cond_favorite, ValueKind.Flag, listOf(ConditionOp.Is)),
    Unread(Res.string.cond_unread, ValueKind.Number, ConditionOp.Counting),
    Progress(Res.string.cond_progress, ValueKind.Number, ConditionOp.Counting),
    Chapters(Res.string.cond_chapters, ValueKind.Number, ConditionOp.Counting),
    Rating(Res.string.cond_rating, ValueKind.Number, ConditionOp.Counting),
    Added(Res.string.cond_added, ValueKind.Number, listOf(ConditionOp.Greater, ConditionOp.Less)),

    // Days since the newest chapter came out, and since a chapter was last read.
    Updated(Res.string.cond_updated, ValueKind.Number, listOf(ConditionOp.Less, ConditionOp.Greater)),
    LastRead(Res.string.cond_last_read, ValueKind.Number, listOf(ConditionOp.Greater, ConditionOp.Less)),
    Source(Res.string.cond_source, ValueKind.Text, ConditionOp.Wording),
    Author(Res.string.cond_author, ValueKind.Text, ConditionOp.Wording),
    Genre(Res.string.cond_genre, ValueKind.Text, ConditionOp.Wording),
    Title(Res.string.cond_title, ValueKind.Text, ConditionOp.Wording),
}

enum class ValueKind { Choice, Number, Text, Flag }

// How a condition compares what it looks at with its value.
enum class ConditionOp(val label: StringResource) {
    Is(Res.string.cond_op_is),
    IsNot(Res.string.cond_op_is_not),
    Contains(Res.string.cond_op_contains),
    NotContains(Res.string.cond_op_not_contains),
    Greater(Res.string.cond_op_greater),
    Less(Res.string.cond_op_less),
    ;

    companion object {
        // The ways to compare a choice, a number and a piece of text.
        val Same = listOf(Is, IsNot)
        val Counting = listOf(Is, IsNot, Greater, Less)
        val Wording = listOf(Contains, NotContains, Is, IsNot)
    }
}
