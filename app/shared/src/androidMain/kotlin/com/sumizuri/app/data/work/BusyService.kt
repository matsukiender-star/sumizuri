package com.sumizuri.app.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.sync.noticeIcon
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.busy_channel
import com.sumizuri.app.shared.resources.busy_more
import com.sumizuri.app.shared.resources.busy_progress
import com.sumizuri.app.shared.resources.busy_stop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val Channel = "busy_work"
private const val Notice = 3
private const val StopAction = "com.sumizuri.app.STOP_WORK"

// How often the notice shows how far the work got.
private const val ShowEveryMs = 1_500L

// Keeps the app running in the background while long work goes on, as Android pauses an app that is out of sight. It shows one quiet notice with the work and a button that stops it, and ends itself once nothing runs.
class BusyService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == StopAction) {
            AppLog.info("work", "stopped from the notice")
            BusyWork.stopAll()
        }
        if (watching?.isActive != true) {
            watching = scope.launch {
                makeChannel()
                // Android wants the notice up at once, so the first one is shown before anything waits.
                ServiceCompat.startForeground(
                    this@BusyService,
                    Notice,
                    notice(BusyWork.tasks.value),
                    if (Build.VERSION.SDK_INT >=
                        29
                    ) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    },
                )
                while (true) {
                    val tasks = BusyWork.tasks.value
                    if (tasks.isEmpty()) break
                    runCatching { NotificationManagerCompat.from(this@BusyService).notify(Notice, notice(tasks)) }
                    delay(ShowEveryMs)
                }
                ServiceCompat.stopForeground(this@BusyService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    // Android gives work of this kind a few hours a day. When they run out, the work goes on as far as the system lets it, without the notice.
    override fun onTimeout(startId: Int, fgsType: Int) {
        AppLog.warn("work", "the system ended the time for work in the background")
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // The first piece of work names the notice, with how far it got, and the others are counted under it.
    private suspend fun notice(tasks: List<BusyTask>): android.app.Notification {
        val first = tasks.firstOrNull()
        val progress = first?.progress?.invoke()?.takeIf { it.second > 0 }
        // A folded notice shows only its title and bar, so the count goes in the title.
        val label = first?.let { getString(it.kind.label) } ?: getString(Res.string.busy_channel)
        val title = progress?.let { "$label · " + getString(Res.string.busy_progress, it.first, it.second) } ?: label
        val builder = NotificationCompat
            .Builder(this, Channel)
            .setSmallIcon(noticeIcon(this))
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(Res.string.busy_stop), stop())
        val lines = buildList {
            first?.detail?.invoke()?.let { add(it) }
            if (tasks.size > 1) add(getString(Res.string.busy_more, tasks.size - 1))
        }
        if (lines.isNotEmpty()) builder.setContentText(lines.joinToString(" · "))
        if (progress !=
            null
        ) {
            builder.setProgress(progress.second, progress.first.coerceAtMost(progress.second), false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private suspend fun makeChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(Channel, getString(Res.string.busy_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun stop(): PendingIntent = PendingIntent.getService(
        this,
        1,
        Intent(this, BusyService::class.java).setAction(StopAction),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openApp(): PendingIntent {
        val intent = (
            packageManager.getLaunchIntentForPackage(
                packageName,
            ) ?: Intent()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            Notice,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        fun start(context: Context) {
            // Android only lets the app start this while it is in sight. Work begun in the background goes on as far as the system lets it.
            runCatching { ContextCompat.startForegroundService(context, Intent(context, BusyService::class.java)) }
                .onFailure { AppLog.debug("work", "could not keep the app awake, ${it.message}") }
        }
    }
}

actual object BusyPlatform {
    actual fun keepAwake() = BusyService.start(AndroidApp.context)
}
