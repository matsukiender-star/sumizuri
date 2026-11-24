package com.sumizuri.app.data.pair

// A TV waiting on the home network for a picture from a phone. Its address, put in a code, opens a page on the phone to pick a photo, and the photo comes back here.
interface PhotoDrop {
    val url: String

    // Waits for the photo the page sends and gives its bytes.
    suspend fun receive(): ByteArray

    fun close()
}

// Starts listening on a free port of the home network with the page to show, or gives null where that cannot be done, such as with no network.
expect fun openPhotoDrop(page: String): PhotoDrop?
