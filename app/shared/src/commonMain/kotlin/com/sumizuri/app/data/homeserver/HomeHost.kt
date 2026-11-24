package com.sumizuri.app.data.homeserver

import okio.Path

// The server as it runs: where this computer reaches it, the certificate phones check, and the setup code until someone claimed it.
interface HomeUp {
    val local: String
    val fingerprint: String
    val setupCode: String?

    // Where phones at home can find this computer now.
    fun addresses(): List<String>
}

// A computer that can be the server of the phones of its person.
interface HomeHost {
    suspend fun start(): HomeUp?
}

// The host of the folder of the app, or nothing on a system that cannot be one.
expect fun homeHostOf(appFolder: Path): HomeHost?
