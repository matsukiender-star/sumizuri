package com.sumizuri.app.feature.player

// Tells a dropped connection from a broken video, from the words the engine gave.
object PlayerErrors {
    private val network = listOf(
        "tcp:", "ffurl_read", "ffurl_", "timed out", "timeout", "connection", "network", "server returned", "http error", "tls:", "ssl", "reset by peer", "end of file", "i/o error", "error in the pull function",
        "io_network", "io_bad_http_status", "bad_http_status", "io_unspecified", "unable to connect", "unresolved",
    )
    private val glitch = listOf(
        "error decoding audio", "error decoding video", "error while decoding", "decoding error", "packet corrupt", "corrupt", "invalid band type", "pes packet size mismatch", "invalid data found", "non-monotonous",
        "decoding_failed", "decoder_failed", "bitstream",
    )

    fun isNetwork(message: String): Boolean = message.lowercase().let { text -> network.any { text.contains(it) } }

    // A secure connection cut as it started, which is how a filter on the network blocks a server by its name.
    fun isCut(message: String): Boolean = message.lowercase().let { "ssl connect error" in it || "sslhandshakeexception" in it }

    // A stream no player can read, such as one a source locks in a way only its own website opens. Starting it again cannot help.
    fun isUnreadable(message: String): Boolean = message.lowercase().let { "treating it as fatal error" in it || "could not find codec parameters" in it }

    // A damaged piece of the stream, such as a bad packet.
    fun isGlitch(message: String): Boolean = message.lowercase().let { text -> glitch.any { text.contains(it) } }
}

// How many times playback is started again quietly after a drop. The count starts again after a long stretch of playing.
class RetryBudget(private val max: Int = 3, private val resetAfterMs: Long = 30_000) {
    private var used = 0
    private var lastAt = 0L

    // Asks for one retry at the position, and says no when the budget is spent.
    fun take(positionMs: Long): Boolean {
        if (used > 0 && (positionMs - lastAt >= resetAfterMs || positionMs < lastAt)) used = 0
        if (used >= max) return false
        used++
        lastAt = positionMs
        return true
    }

    fun reset() {
        used = 0
        lastAt = 0
    }
}
