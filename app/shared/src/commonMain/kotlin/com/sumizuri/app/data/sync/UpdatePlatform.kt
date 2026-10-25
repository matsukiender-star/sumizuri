package com.sumizuri.app.data.sync

import androidx.compose.runtime.Composable
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.notice_more
import com.sumizuri.app.shared.resources.notice_new_chapters
import com.sumizuri.app.shared.resources.notice_new_line
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

// What the system does for the checks of the library besides the app: running them while the app is closed, and telling about new chapters.
expect object UpdatePlatform {
    // Whether the system can run checks while the app is closed.
    val runsInBackground: Boolean

    // Whether the app shows the notice itself after a check it ran on its own, because nothing else would.
    val noticeFromApp: Boolean

    // Plans the check for while the app is closed as the settings say, or stops it when it is off or the hours are zero.
    fun plan(plan: UpdatePlan)

    // Tells the person about new chapters a check found. Nothing is shown when there are none.
    suspend fun showNews(news: List<ChapterNews>)
}

// When and how the check of the library runs while the app is closed, as the settings say.
data class UpdatePlan(val hours: Int, val inBackground: Boolean, val wifiOnly: Boolean, val charging: Boolean)

// Gives a way to ask the person to allow notices, where the system wants that asked. It does nothing where it is not needed.
@Composable
expect fun rememberNoticePermission(): () -> Unit

// The words of a notice about new chapters: the headline with the count, and a line for each title with the most new chapters first, ending with how many more titles there are.
internal suspend fun newsWords(news: List<ChapterNews>, lines: Int = 5): Pair<String, List<String>> {
    val total = news.sumOf { it.added }
    val head = getPluralString(Res.plurals.notice_new_chapters, total, total)
    val shown = news.take(lines).map { getString(Res.string.notice_new_line, it.name, it.added) }
    val more = if (news.size > lines) listOf(getString(Res.string.notice_more, news.size - lines)) else emptyList()
    return head to shown + more
}
