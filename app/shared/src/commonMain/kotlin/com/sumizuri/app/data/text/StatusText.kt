package com.sumizuri.app.data.text

import com.sumizuri.app.data.model.Status

// Reads the free text a source gives for the state of a title.
fun statusFromText(text: String?): Status {
    val word = text?.lowercase() ?: return Status.Unknown
    return when {
        "hiatus" in word -> Status.Hiatus
        "cancel" in word || "drop" in word -> Status.Cancelled
        "complete" in word || "finish" in word || "ended" in word -> Status.Completed
        "ongoing" in word || "publishing" in word || "releasing" in word || "airing" in word -> Status.Ongoing
        else -> Status.Unknown
    }
}

// A whole number reads without a decimal, so 12.0 is 12 and 12.5 stays 12.5.
fun Double.numberText(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
