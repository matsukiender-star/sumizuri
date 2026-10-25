package com.sumizuri.app.data.blocks

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.blocks_blank
import com.sumizuri.app.shared.resources.blocks_finished
import com.sumizuri.app.shared.resources.blocks_goal
import com.sumizuri.app.shared.resources.blocks_heatmap
import com.sumizuri.app.shared.resources.blocks_most_read
import com.sumizuri.app.shared.resources.blocks_recent
import com.sumizuri.app.shared.resources.blocks_top_genres
import com.sumizuri.app.shared.resources.blocks_weekdays
import com.sumizuri.app.shared.resources.choice_figures
import com.sumizuri.app.shared.resources.choice_year
import com.sumizuri.app.shared.resources.library_continue
import com.sumizuri.app.shared.resources.profile_chapters_read
import com.sumizuri.app.shared.resources.profile_stats
import com.sumizuri.app.shared.resources.stats_current_streak
import com.sumizuri.app.shared.resources.stats_per_active_day
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.random.Random

private val Pretty = Json { prettyPrint = true }

private inline fun script(build: JsonObjectBuilder.() -> Unit): String =
    Pretty.encodeToString(JsonObject.serializer(), buildJsonObject(build))

// A part of the profile as it was, as a script of its one view.
internal fun sectionScript(name: String) = script { putJsonObject("view") { put("section", name) } }

fun newBlockId(): String = "b" + Random.nextLong().toULong().toString(36)

// The blocks a person can add, each a script to start from, with the size it starts at. The parts of the profile as it was are among them, so one that was taken away can come back.
enum class BlockTemplate(val label: StringResource, val w: Int, val h: Int) {
    Blank(Res.string.blocks_blank, 2, 1),
    Streak(Res.string.stats_current_streak, 2, 1),
    Pace(Res.string.stats_per_active_day, 2, 1),
    Goal(Res.string.blocks_goal, 4, 2),
    Finished(Res.string.blocks_finished, 2, 1),
    Genres(Res.string.blocks_top_genres, 4, 0),
    Weekdays(Res.string.blocks_weekdays, 4, 0),
    Heatmap(Res.string.blocks_heatmap, 4, 0),
    Recent(Res.string.blocks_recent, 4, 0),
    MostRead(Res.string.blocks_most_read, 4, 0),
    Figures(Res.string.choice_figures, 4, 0),
    Year(Res.string.choice_year, 4, 0),
    Continue(Res.string.library_continue, 4, 0),
    Statistics(Res.string.profile_stats, 4, 0),
    ;

    // The script of the template, made by the simple editor so it opens there. Labels are written in the language of the app at the time, and can be changed like the rest.
    suspend fun script(): String {
        val name = getString(label)
        return when (this) {
            Blank -> SimpleBlock(value = "stats.read", label = getString(Res.string.profile_chapters_read)).script()

            Streak -> SimpleBlock(value = "stats.streak", label = name).script()

            Pace -> SimpleBlock(
                value = "formula",
                formula = "round(stats.read / max(stats.days, 1), 1)",
                label = name,
            ).script()

            Goal -> SimpleBlock(show = "ring", value = "stats.month", goal = 100.0, label = name).script()

            Finished -> SimpleBlock(value = "finished", label = name).script()

            Genres -> SimpleBlock(
                show = "chart",
                source = "library",
                by = "genre",
                top = 6,
                style = "bars",
                label = name,
            ).script()

            Weekdays -> SimpleBlock(
                show = "chart",
                source = "reads",
                by = "weekday",
                style = "columns",
                label = name,
            ).script()

            Heatmap -> SimpleBlock(show = "heatmap", days = 365, weeks = 26, label = name).script()

            Recent -> SimpleBlock(show = "covers", titles = "recent", days = 30, count = 12, label = name).script()

            MostRead -> SimpleBlock(
                show = "list",
                source = "reads",
                by = "title",
                days = 90,
                top = 5,
                label = name,
            ).script()

            Figures -> sectionScript("figures")

            Year -> sectionScript("year")

            Continue -> sectionScript("continue")

            Statistics -> sectionScript("statistics")
        }
    }

    suspend fun block(): ProfileBlock = ProfileBlock(newBlockId(), w, h, script())
}

// The profile as it was before blocks: the figures, the year of reading, what is being read and the statistics, each the whole width.
fun defaultLayout(): BlockLayout = BlockLayout(
    blocks = listOf("figures", "year", "continue", "statistics").map {
        ProfileBlock("classic-$it", 4, 0, sectionScript(it))
    },
)
