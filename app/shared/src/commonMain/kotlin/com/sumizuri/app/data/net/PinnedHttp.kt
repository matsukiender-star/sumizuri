package com.sumizuri.app.data.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

// A client that talks https only, and only to the one certificate with this SHA-256, for a server at home that no authority can vouch for.
expect fun pinnedHttpClient(fingerprint: String, configure: HttpClientConfig<*>.() -> Unit): HttpClient
