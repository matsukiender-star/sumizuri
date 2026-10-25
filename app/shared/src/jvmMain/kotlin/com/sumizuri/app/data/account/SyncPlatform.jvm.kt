package com.sumizuri.app.data.account

// A computer syncs while the app runs, also from the tray, so nothing is planned for while it is closed.
actual object SyncPlatform {
    actual fun plan(plan: SyncPlan) = Unit
}
