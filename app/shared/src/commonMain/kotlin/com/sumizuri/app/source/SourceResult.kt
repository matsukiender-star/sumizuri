package com.sumizuri.app.source

sealed interface SourceResult<out T> {
    data class Ok<T>(val value: T) : SourceResult<T>
    data class Err(val error: SourceError) : SourceResult<Nothing>
}

// The value of a call that worked, or null when it failed.
fun <T> SourceResult<T>.valueOrNull(): T? = (this as? SourceResult.Ok)?.value

inline fun <T, R> SourceResult<T>.map(transform: (T) -> R): SourceResult<R> = when (this) {
    is SourceResult.Ok -> SourceResult.Ok(transform(value))
    is SourceResult.Err -> this
}

// Why a call to a source failed, in a form the screens can show.
sealed class SourceError(val message: String) {
    // The source does not offer this call, so the feature should be hidden and not shown as broken.
    class NotImplemented(val method: String) : SourceError("This source does not support $method.")

    // The site could not be reached, or it answered with an error.
    class Network(message: String) : SourceError(message)

    // A bot check answered instead of the page.
    class Challenge(val url: String, message: String) : SourceError(message)

    // The page or reply did not look the way the source expected.
    class Parse(message: String) : SourceError(message)

    // The extension's own code failed, or its definition is broken.
    class Script(message: String) : SourceError(message)

    // The host refused the request, for example a private address.
    class Blocked(message: String) : SourceError(message)
}
