package com.sumizuri.app.data.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.core.log.AppLog

// The button on the notice of a running check. It asks the check to stop, the way the stop button of the app does: the titles being asked finish and no new one starts. The planned checks after it stay as they were.
class StopCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLog.info("updates", "the check was stopped from its notice")
        AndroidApp
            .profiles()
            .container.value.sync
            .cancelRefresh()
    }

    companion object {
        // What the button sends. Only this app can send it, since the receiver is not shared with others.
        fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, StopCheckReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
