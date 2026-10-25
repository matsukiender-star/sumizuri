package com.sumizuri.app.data.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// The network of an iPhone or iPad takes no router from the app yet, so the network page is not shown there.
actual object NetworkEngine {
    actual val states: StateFlow<Map<String, LinkState>> = MutableStateFlow(emptyMap())

    actual fun startRouter(): Int? = null

    actual fun stopRouter() = Unit

    actual fun keep(inUse: List<Connection>) = Unit

    actual suspend fun trace(connection: Connection?): Result<Pair<String, String>> =
        Result.failure(UnsupportedOperationException())
}

internal actual fun applyRouter(port: Int?) = Unit

actual val networkSupported: Boolean = false
