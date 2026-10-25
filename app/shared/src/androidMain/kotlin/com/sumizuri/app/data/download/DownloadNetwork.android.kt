package com.sumizuri.app.data.download

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sumizuri.app.AndroidApp

actual fun onUnmeteredNetwork(): Boolean {
    val manager = AndroidApp.context.getSystemService(ConnectivityManager::class.java) ?: return true
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}

actual val canTellNetwork: Boolean = true
