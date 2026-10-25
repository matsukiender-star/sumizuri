package com.sumizuri.app.data.download

// The system does not tell an app whether the connection is metered without a monitor of the network, which is not written yet. Until then every connection counts as one that is not.
actual fun onUnmeteredNetwork(): Boolean = true

actual val canTellNetwork: Boolean = false
