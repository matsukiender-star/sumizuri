package com.sumizuri.app.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.notice_channel_checking
import com.sumizuri.app.shared.resources.notice_channel_new
import org.jetbrains.compose.resources.getString
import java.util.concurrent.TimeUnit

// The name the planned check goes by, so planning it again replaces it.
private const val WorkName = "library-updates"

internal const val NewsChannel = "new_chapters"
internal const val CheckingChannel = "library_checks"
private const val NewsNotice = 1

actual object UpdatePlatform {
    actual val runsInBackground: Boolean = true

    // The check that runs while the app is closed shows its own notice, so the app does not show one too.
    actual val noticeFromApp: Boolean = false

    actual fun plan(plan: UpdatePlan) {
        val (hours, inBackground, wifiOnly, charging) = plan
        val work = WorkManager.getInstance(AndroidApp.context)
        if (!inBackground || hours <= 0) {
            work.cancelUniqueWork(WorkName)
            AppLog.info("updates", "no check while the app is closed")
            return
        }
        val constraints = Constraints
            .Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(charging)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<LibraryUpdateWorker>(
            hours.toLong(),
            TimeUnit.HOURS,
        ).setConstraints(constraints).build()
        work.enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.UPDATE, request)
        AppLog.info(
            "updates",
            "check while the app is closed every $hours hours${if (wifiOnly) ", on wifi" else ""}${if (charging) ", while charging" else ""}",
        )
    }

    actual suspend fun showNews(news: List<ChapterNews>) {
        if (news.isEmpty()) return
        val context = AndroidApp.context
        if (!canNotify(context)) return
        makeChannels(context)
        val (head, lines) = newsWords(news)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(head)
        lines.forEach { style.addLine(it) }
        val notice = NotificationCompat
            .Builder(context, NewsChannel)
            .setSmallIcon(noticeIcon(context))
            .setContentTitle(head)
            .setContentText(lines.joinToString(", "))
            .setStyle(style)
            .setContentIntent(openUpdates(context))
            .setGroup(NewsGroup)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        showTitleNews(context, news)
        NotificationManagerCompat.from(context).notify(NewsNotice, notice)
    }
}

// Whether the app may show notices, which on Android 13 and newer the person has to allow.
@Suppress("MissingPermission")
internal fun canNotify(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

// The two kinds of notice, made once, so the person can turn each off on its own in the settings of the system. Android before 8 has no kinds.
internal suspend fun makeChannels(context: Context) {
    if (Build.VERSION.SDK_INT < 26) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    manager.createNotificationChannel(
        NotificationChannel(
            NewsChannel,
            getString(Res.string.notice_channel_new),
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
    )
    manager.createNotificationChannel(
        NotificationChannel(
            CheckingChannel,
            getString(Res.string.notice_channel_checking),
            NotificationManager.IMPORTANCE_LOW,
        ),
    )
}

// The small icon of the app for notices, or one of the system when the app has none.
internal fun noticeIcon(context: Context): Int =
    context.resources.getIdentifier("ic_notification", "drawable", context.packageName).takeIf { it != 0 }
        ?: android.R.drawable.stat_notify_sync

// Opens the app on the tab of updates, the way the shortcut of the launcher does.
private fun openUpdates(context: Context): PendingIntent {
    val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
        putExtra("tab", "Updates")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        context,
        NewsNotice,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

@Composable
actual fun rememberNoticePermission(): () -> Unit {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { allowed -> AppLog.info("updates", "notices ${if (allowed) "allowed" else "not allowed"}") }
    return {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
