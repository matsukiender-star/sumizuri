package com.sumizuri.app.data.download

// Whether the device is on Wi-Fi or another connection that is not metered. Where the system cannot tell, as on a computer, it counts as one.
expect fun onUnmeteredNetwork(): Boolean

// Whether the system can tell the kind of connection, so the choice of Wi-Fi only means something.
expect val canTellNetwork: Boolean
