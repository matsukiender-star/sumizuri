package com.sumizuri.app.data.account

import com.sumizuri.sync.core.DeviceInfo

// What this device tells a sync server about itself: its name and kind, which the owner sees in the list of devices.
expect fun thisDevice(): DeviceInfo
