@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.source.host.net

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.AF_UNSPEC
import platform.posix.NI_NUMERICHOST
import platform.posix.SOCK_STREAM
import platform.posix.addrinfo
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.getnameinfo

// The most characters the text of an address takes.
private const val AddressRoom = 1025

// The lookup the system itself does, with the addresses it finds written out as text.
actual object SystemResolver : HostResolver {
    actual override suspend fun resolve(host: String): List<String> = withContext(Dispatchers.Default) {
        memScoped {
            val hints = alloc<addrinfo>().apply {
                ai_family = AF_UNSPEC
                ai_socktype = SOCK_STREAM
            }
            val found = alloc<CPointerVar<addrinfo>>()
            // The request itself will fail and say why, so a name that is not found is an empty list.
            if (getaddrinfo(host, null, hints.ptr, found.ptr) != 0) return@memScoped emptyList()
            val text = allocArray<ByteVar>(AddressRoom)
            val addresses = mutableListOf<String>()
            var entry = found.value
            while (entry != null) {
                val info = entry.pointed
                if (getnameinfo(
                        info.ai_addr,
                        info.ai_addrlen,
                        text,
                        AddressRoom.convert(),
                        null,
                        0.convert(),
                        NI_NUMERICHOST,
                    ) ==
                    0
                ) {
                    addresses += text.toKString()
                }
                entry = info.ai_next
            }
            freeaddrinfo(found.value)
            addresses
        }
    }
}
