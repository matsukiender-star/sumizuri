package com.sumizuri.app.data.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

// Pairing with a computer at home is not on iPhone yet, and never falls back to a client that would trust anyone.
actual fun pinnedHttpClient(fingerprint: String, configure: HttpClientConfig<*>.() -> Unit): HttpClient =
    throw UnsupportedOperationException("Pairing with a computer is not supported on this system yet.")
