package com.sumizuri.app.data.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_cancel_update
import com.sumizuri.app.shared.resources.notice_checking
import com.sumizuri.app.shared.resources.notice_checking_progress
import com.sumizuri.app.shared.resources.notice_checking_stopping
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val CheckingNotice = 2

// How often the notice of a running check shows how far it got.
private const val ProgressEveryMs = 2_000L

// A planned check may run this much before its hour is full, since the system picks the moment within a window.
private const val SlackMs = 15 * 60 * 1000L

// Checks the library of the open profile while the app is closed, as the system planned it, and tells about the new chapters it found.
class LibraryUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = AndroidApp.profiles().container.value
        val sync = container.sync
        // The backup the app makes by itself comes along with this check while the app is closed, when it is due.
        container.autoBackups.runIfDue()
        // A check the app runs right now, or one that ran a short while ago, is not done again.
        if (!sync.isDue(SlackMs)) return Result.success()
        makeChannels(applicationContext)
        showChecking(0, 0)
        AppLog.info("updates", "check while the app is closed")
        val ran = coroutineScope {
            val shown = launch {
                while (true) {
                    delay(ProgressEveryMs)
                    sync.progress.value?.let { showChecking(it.done, it.total) }
                }
            }
            try {
                sync.refreshLibrary(keepAwake = false)
                true
            } finally {
                shown.cancel()
            }
        }
        if (ran &&
            container.settings.flag(SettingKey.UpdateNotify, true).first()
        ) {
            UpdatePlatform.showNews(sync.lastNews())
        }
        return Result.success()
    }

    // Shows how far the check is in a quiet notice. A check that runs long needs one to keep going, but Android may not allow it while the app is in the background, and then the check goes on without it.
    private suspend fun showChecking(done: Int, total: Int) {
        try {
            setForeground(checkingInfo(done, total))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLog.debug("updates", "the check runs without its notice, ${error.message}")
        }
    }

    // The notice has a button that stops the check. Once it is pressed the notice says the check is stopping, while the titles being asked finish.
    private suspend fun checkingInfo(done: Int, total: Int): ForegroundInfo {
        val stopping = AndroidApp
            .profiles()
            .container.value.sync.stopping
        val notice = NotificationCompat
            .Builder(applicationContext, CheckingChannel)
            .setSmallIcon(noticeIcon(applicationContext))
            .setContentTitle(getString(Res.string.notice_checking))
            .apply {
                when {
                    stopping -> setContentText(getString(Res.string.notice_checking_stopping)).setProgress(0, 0, true)

                    total > 0 -> setContentText(
                        getString(Res.string.notice_checking_progress, done, total),
                    ).setProgress(total, done, false)

                    else -> setProgress(0, 0, true)
                }
                if (!stopping) {
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(Res.string.library_cancel_update),
                        StopCheckReceiver.pending(applicationContext),
                    )
                }
            }.setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >=
            29
        ) {
            ForegroundInfo(CheckingNotice, notice, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(CheckingNotice, notice)
        }
    }
}
