package com.sumizuri.app.data.net

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.sumizuri.app.core.log.AppLog

// Android 17 is the first that keeps apps off the home network until the person allows it.
private const val FirstGuardedSdk = 37

@Composable
actual fun rememberLocalNetworkAccess(): (then: () -> Unit) -> Unit {
    val context = LocalContext.current
    var waiting by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        AppLog.info("network", "home network ${if (allowed) "allowed" else "not allowed"}")
        waiting?.invoke()
        waiting = null
    }
    return { then ->
        if (Build.VERSION.SDK_INT < FirstGuardedSdk ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            then()
        } else {
            waiting = then
            launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
