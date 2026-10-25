package com.sumizuri.app.data.sync

import androidx.compose.runtime.Composable
import platform.UserNotifications.*

// The check of the library while the app is closed needs a task the system schedules, which is not written yet. The app checks while it is open and shows the notice itself.
actual object UpdatePlatform {
    actual val runsInBackground: Boolean = false

    actual val noticeFromApp: Boolean = true

    actual fun plan(plan: UpdatePlan) = Unit

    actual suspend fun showNews(news: List<ChapterNews>) {
        if (news.isEmpty()) return
        val (head, lines) = newsWords(news, lines = 3)
        val content = UNMutableNotificationContent()
        content.setTitle(head)
        content.setBody(lines.joinToString("\n"))
        val request = UNNotificationRequest.requestWithIdentifier("sumizuri-news", content, trigger = null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            request,
            withCompletionHandler = null,
        )
    }
}

// Notices have to be allowed by the person once, and the system asks only the first time.
@Composable
actual fun rememberNoticePermission(): () -> Unit = {
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionBadge,
    ) { _, _ -> }
}
