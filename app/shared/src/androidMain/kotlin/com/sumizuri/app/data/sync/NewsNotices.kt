package com.sumizuri.app.data.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.toBitmap
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.navigation.AppLinks
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.notice_mark_read
import com.sumizuri.app.shared.resources.notice_new_chapters
import com.sumizuri.app.shared.resources.notice_read
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

// The notices of single titles fold under the summary of the check.
internal const val NewsGroup = "sumizuri.news"

// How many titles get a notice of their own. The rest are only in the summary.
private const val MostTitles = 6

// How big the cover on a notice is read.
private const val CoverPx = 192

// A notice for each title with new chapters, with its cover, how many came, and buttons to read on and to mark them read. They fold under the summary of the check.
internal suspend fun showTitleNews(context: Context, news: List<ChapterNews>) {
    val manager = NotificationManagerCompat.from(context)
    for (item in news.take(MostTitles)) {
        val notice = NotificationCompat
            .Builder(context, NewsChannel)
            .setSmallIcon(noticeIcon(context))
            .setContentTitle(item.name)
            .setContentText(getPluralString(Res.plurals.notice_new_chapters, item.added, item.added))
            .setLargeIcon(coverOf(context, item.coverUrl))
            .setGroup(NewsGroup)
            .setContentIntent(openLink(context, AppLinks.titleLink(item.titleId, read = false), item.titleId * 2))
            .addAction(
                0,
                getString(Res.string.notice_read),
                openLink(
                    context,
                    AppLinks.titleLink(item.titleId, read = true),
                    item.titleId * 2 + 1,
                ),
            ).addAction(0, getString(Res.string.notice_mark_read), NewsReceiver.markRead(context, item.titleId))
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        manager.notify(noticeIdOf(item.titleId), notice)
    }
}

// The number of the notice of a title, kept apart from the other notices of the app.
internal fun noticeIdOf(titleId: Long): Int = 10_000 + (titleId % 1_000_000).toInt()

private suspend fun coverOf(context: Context, url: String?): Bitmap? {
    if (url == null) return null
    val request = ImageRequest
        .Builder(context)
        .data(url)
        .size(Size(CoverPx, CoverPx))
        .build()
    val result =
        runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull() as? SuccessResult ?: return null
    return runCatching { result.image.toBitmap() }.getOrNull()
}

// Opens a link of the app in it, which the app reads like any sumizuri link.
private fun openLink(context: Context, link: String, code: Long): PendingIntent {
    val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(link)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return PendingIntent.getActivity(
        context,
        code.toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

// Marks every chapter of a title read from its notice, and takes the notice away.
class NewsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val titleId = intent.getLongExtra(TitleExtra, -1)
        if (titleId < 0) return
        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val library = AndroidApp
                    .profiles()
                    .container.value.library
                library.setReadMany(titleId, library.chapterIds(titleId), true)
                NotificationManagerCompat.from(context).cancel(noticeIdOf(titleId))
                AppLog.info("updates", "the new chapters of a title were marked read from its notice")
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        private const val TitleExtra = "titleId"

        // What the button sends. Only this app can send it, since the receiver is not shared with others.
        fun markRead(context: Context, titleId: Long): PendingIntent = PendingIntent.getBroadcast(
            context,
            noticeIdOf(titleId),
            Intent(context, NewsReceiver::class.java).putExtra(TitleExtra, titleId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
