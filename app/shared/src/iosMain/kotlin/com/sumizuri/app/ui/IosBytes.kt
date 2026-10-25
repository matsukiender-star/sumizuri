@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

internal fun NSData.toByteArray(): ByteArray {
    val out = ByteArray(length.toInt())
    if (out.isNotEmpty()) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}

internal fun NSData.asText(): String? = NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()
