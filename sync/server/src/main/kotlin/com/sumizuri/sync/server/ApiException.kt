package com.sumizuri.sync.server

import com.sumizuri.sync.core.ErrorCodes
import io.ktor.http.HttpStatusCode

// A request the server turns down on purpose, answered as an ApiError with this status.
class ApiException(val status: HttpStatusCode, val code: String, message: String) : RuntimeException(message)

fun badRequest(message: String): Nothing = throw ApiException(HttpStatusCode.BadRequest, ErrorCodes.BadRequest, message)
